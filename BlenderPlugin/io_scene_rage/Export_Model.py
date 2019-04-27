# ##### BEGIN GPL LICENSE BLOCK #####
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
#
# ##### END GPL LICENSE BLOCK #####

import bpy
import bmesh
import mathutils
from array import array


#=================================================================
#
#		RAGE Skeletal Model Mesh export script
#		This model format was meant for use with the RAGE game engine.
#
#		This script exports a Blender Mesh object in the following tab-delimited format:
#
# 		vertCount	triCount	boneCount
#		vert1PosX	vert1PosY	vert1PosZ	vert1UVX	vert1UVY
#				... vert0Bone0Weight	vert0Bone0Index	vert0Bone1Weight	vert0Bone1Index	vert0Bone2Weight	vert0Bone2Index
#				... vert1NormalX	vert1NormalY	vert1NormalZ
#				... vert1TangentX	vert1TangentY	vert1TangentZ
#				... vert1BinormalX	vert1BinormalY	vert1BinormalZ
#		vert2PosX ... etc
#		tri1Vert1	tri1Vert2	tri1Vert3
#		triNVert1 ... etc
#
#		Installation:
#		1) Copy the script into your Blender addons folder
#		2) go into File -> User Preferences -> Add-ons -> Install from File
#		3) Navigate to where you pasted the script and select it.
#		4) Enable the script from the Addons menu.
#		5) Save user preferences.


#		Usage:
#		Simply select a Mesh object
#		Then select: File -> Export -> "RAGE Skeletal Model Mesh (.rkm)"
#		Choose an output directory and filename
#		Click "Export Model"
#
#		Author: Luis Gutierrez
#
#=================================================================

bl_info = {
	"name": "RAGE Skeletal Model Mesh (.rkm)",
	"author": "Luis Gutierrez",
	"version": (1, 0),
	"blender": (2, 6, 4),
	"location": "File > Import-Export > RKM",
	"description": "Export RAGE Skeletal Mesh File",
	"warning": "",
	"wiki_url": "",
	"tracker_url": "",
	"support": "COMMUNITY",
	"category": "Import-Export"
}


# Blender coordinate system is y-axis up, RAGE is z-axis up
# construct a matrix that flips the coordinate system to resolve this for all model values
# Blender: z-up, x-right, y-forward
# RAGE: z-forward, x-right, y-up
# This is the corresponding change-of-basis matrix to go from
# Blender coordinates to RAGE coordinates
mat_world = mathutils.Matrix(((1.0,0.0,0.0),(0.0,0.0,1.0),(0.0,-1.0,0.0)))

# Getting the inverse-transpose of mat_world for transforming
# normals, tangents, and binormals
mat_world_IT = mat_world.copy()
mat_world_IT.invert()
mat_world_IT.transpose()



def export_rage_mesh(context, filepath):
	print("=============== Starting script ===========")

	# The active object must be a mesh
	if bpy.context.object.type != 'MESH':
		print("ERROR: A mesh must be selected to export a model")
		return {'FINISHED'}

	ob = bpy.context.object

	# The object must have a UV-map
	if len(ob.data.uv_layers) == 0:
		print("ERROR: The selected Object must have a UV Map.")
		return {'FINISHED'}

	parent = ob.parent
	# Making sure this object's parent is an Armature
	if parent is None or parent.type != 'ARMATURE':
		print("ERROR: The selected Object's parent must be an Armature.")
		return {'FINISHED'}

	f = open(filepath,'w')

	#Backup the currently selected object to reselect it after we're done
	active_object = bpy.context.scene.objects.active

	#=====================================================
	#   Triangulating the Mesh
	#=====================================================
	# This model format only support triangles, but Blender supports quads and Ngons (polygons with any number of vertices)
	# We have to convert the model to use tris only, but we don't want to write to the original model.
	# Therefore, we need to duplicate the model, triangulate the mesh, export that, then delete that temporary mesh

	#Duplicate object to create temp object to triangulate
	bpy.ops.object.duplicate_move()
	#Go to edit mode
	bpy.ops.object.mode_set(mode='EDIT')
	#Make sure no faces are hidden (so that we triangulate all of the faces)
	bpy.ops.mesh.reveal()
	#Make sure we are selecting everything
	bpy.ops.mesh.select_all(action='SELECT')
	#Triangulate the mesh
	bpy.ops.mesh.quads_convert_to_tris(quad_method='BEAUTY', ngon_method='BEAUTY')
	#Return to object mode
	bpy.ops.object.mode_set(mode='OBJECT')
	#=====================================================

	#=====================================================
	#   Getting bone data for vertex bone indices
	#=====================================================

	ob = bpy.context.object
	bone_names = []
	# Getting the list of bone names to use for the parent bone index
	# Getting the armature object
	armature = ob.parent
	# This assumes the object is parented to the Armature object
	# We have already checked for this

	#Adding all armature bone names to the list
	for bone in armature.data.bones:
		bone_names.append(bone.name)
	#=====================================================


	#=====================================================
	#   Getting the actual mesh data
	#=====================================================
	# Tuples of vertex data, one entry per unique vertex data tuple
	unique_verts = []
	# Tuples of indices to vertices in unique_verts array
	triangles = []

	# Calculate tangents
	ob.data.calc_tangents(ob.data.uv_layers[0].name)

	# Calculating numbers for printing the current progress to console
	total_progress = len(ob.data.polygons)
	cur_progress = 0
	total_num_chars = 40
	char_per_progress = float(total_num_chars) / float(total_progress)
	# Only print num chars when num_chars changes
	last_num_chars = -1

	for face in ob.data.polygons:
		# Printing the current export progress
		num_chars = int(char_per_progress * cur_progress)
		if num_chars != last_num_chars:
			print("[" + (num_chars * "=") + ((total_num_chars - num_chars) * " ") + "]")
			last_num_chars = num_chars
		cur_progress += 1
		# Indices of triangle vertices
		tri_verts = [0,0,0]
		i = 0
		#zip just returns a tuple containing ith elements from both lists at the same time
		for vert_index, loop_vert_index in zip(face.vertices, face.loop_indices):
			vert = ob.data.vertices[vert_index]
			loop_vert = ob.data.loops[loop_vert_index]

			#=============================
			# Getting vertex position
			#=============================
			vert_pos = vert.co

			# Getting the vertex texture coordinates
			vert_uv_coord = ob.data.uv_layers.active.data[loop_vert_index].uv

			#=============================
			# Getting vertex normal
			#=============================
				# if face.use_smooth is true, the vertex's normal is the normalized average of all faces it is a part of
				# if it's false, the vertex's normal is the face normal that it's a part of.
				# such that all vertices that are a part of that face share the same normal.
			vert_normal = (0,0,1)
			vert_tangent = (0,0,1)
			vert_binormal = (0,0,1)
			if(face.use_smooth):
				#Use vert normal as vert normal
				vert_normal = vert.normal.copy()
				vert_tangent = loop_vert.tangent.copy()
				vert_binormal = vert_normal.cross(vert_tangent)
			else:
				#Use face normal as vert normal
				vert_normal = face.normal.copy()
				#ensuring that the tangent lies in the plane of the triangle (orthogonal to the normal)
				vert_tangent = loop_vert.tangent.copy()
				vert_tangent = vert_tangent - (vert_normal * vert_normal.dot(vert_tangent))
				vert_binormal = vert_normal.cross(vert_tangent)
			# Ensuring these vectors are unit length
			vert_normal.normalize()
			vert_tangent.normalize()
			vert_binormal.normalize()

			#=============================
			# Getting vertex bone data
			#=============================
			# We only support up to 3 bones per vertex.
			# Among all of the groups the vertex is a part of, we only choose the groups with the top 3 vertex weights
			# If the vertex is not a part of any groups (or has a total weight of 0 across all groups)
			vert_bone_data = []

			# Iterate through each group that this vertex is a member of
			for grp in vert.groups:
				bone_name = ob.vertex_groups[grp.group].name

				#If the vertex group does not correspond to a bone name, skip it
				if bone_name not in bone_names:
					print("Mesh has the vertex group \"%s\", but there is no bone in the parent Armature with that name." % (bone_name))
					cleanup()
					continue

				bone_index = bone_names.index(bone_name)
				bone_weight = grp.weight

				vert_bone_data.append( [bone_weight, bone_index] )

			# Only use the top-3 bone weights
			vert_bone_data.sort(reverse=True)
			vert_bone_data = vert_bone_data[:3]

			# Padding with 0-weight bones
			vert_bone_data += [[0,0]] * (3 - len(vert_bone_data))

			# If the bone weights don't add up to 0, normalize them to 1
			sum_of_weights = 0
			for bone_datum in vert_bone_data:
				sum_of_weights += bone_datum[0]
			if sum_of_weights != 0:
				# Normalize the bone weights
				for bone_datum in vert_bone_data:
					bone_datum[0] /= sum_of_weights
			#========================================================
			# Transforming the vertex data to RAGE coordinate system
			#========================================================
			vert_pos = mat_world * vert_pos
			vert_normal = mat_world_IT * vert_normal
			vert_tangent = mat_world_IT * vert_tangent
			vert_binormal = mat_world_IT * vert_binormal

			#=================================
			# Building the vertex definition
			#=================================

			# first 3 numbers are Vertex position
			vertex = [ vert_pos[0], vert_pos[1], vert_pos[2] ]
			# Next 2 numbers are Vertex tex coord
			vertex += [ vert_uv_coord[0], vert_uv_coord[1] ]
			# Next 6 numbers are 3 x (bone weight and bone index)
			vertex += [ vert_bone_data[0][0], vert_bone_data[0][1], vert_bone_data[1][0], vert_bone_data[1][1], vert_bone_data[2][0], vert_bone_data[2][1] ]
			# Next 3 numbers are vert normal
			vertex += [ vert_normal[0], vert_normal[1], vert_normal[2] ]
			# Next 3 numbers are vert tangent
			vertex += [ vert_tangent[0], vert_tangent[1], vert_tangent[2] ]
			# Next 3 numbers are vert binormal
			vertex += [ vert_binormal[0], vert_binormal[1], vert_binormal[2] ]

			# Add this vertex to our list of unique verts if it is not in the list already
			if vertex not in unique_verts:
				unique_verts.append(vertex)
			# Get the index of this vertex in the list and set it as the triangle's vert index
			tri_verts[i] = unique_verts.index(vertex)
			i += 1
		triangles.append(tri_verts)
	ob.data.free_tangents()

	#=====================================================
	#   Writing data to file
	#=====================================================

	# Writing vertex count, triangle count, and bone count to first line
	header_data = [len(unique_verts), len(triangles), len(bone_names)]
	f.write( "\t".join(map(str,header_data)) + "\n")

	# Iterate through all vertices writing all of the vert data
	for vert in unique_verts:
		f.write( "\t".join(map(str,vert)) + "\n")

	# Iterate through all triangles writing all of the tri data
	for tri in triangles:
		f.write( "\t".join(map(str,tri)) + "\n")

	f.close()

	#Destroy the temp triangulated object
	bpy.ops.object.delete()

	#Setting the original object as the active object again
	bpy.context.scene.objects.active = active_object
	active_object.select = True

	print("File saved to \"%s\"" % filepath)
	return{'FINISHED'}




#ExportHelper is a helper class, defines filename and invoke() function which calls
# file selector.
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

class ExportRAGEMesh(Operator, ExportHelper):
	"""This is the tooltip of the operator and in generated docs"""
	bl_idname = "export_rage_skeletal.mesh" #this is how bpy.ops_import_test.some_data is constructed
	bl_label = "Export Model"

	#ExportHelper mixin class uses this
	#For model file
	filename_ext = ".rkm"

	filter_glob = StringProperty(
		default="*.rkm",
		options={'HIDDEN'},
		maxlen=255, #Maxinternal buffer length, longer would be clamped.
		)
	#List of operator properties, the attributes will be assigned
	# to the class instance from the operator settings before calling.

	def execute(self, context):
		return export_rage_mesh(context, self.filepath)
#Only needed if you want to add into dynamic menu
def menu_func_export(self,context):
	self.layout.operator(ExportRAGEMesh.bl_idname, text="RAGE Skeletal Model Mesh (.rkm)")

def register():
	bpy.utils.register_class(ExportRAGEMesh)
	bpy.types.INFO_MT_file_export.append(menu_func_export)

def unregister():
	bpy.utils.unregister_class(ExportRAGEMesh)
	bpy.types.INFO_MT_file_export.remove(menu_func_export)

if __name__ == "__main__":
	register()


	#Uncomment this line to actually run the exporter on script execution
	bpy.ops.export_rage_skeletal.mesh('INVOKE_DEFAULT')
