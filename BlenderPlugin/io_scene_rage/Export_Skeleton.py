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
from array import array
import mathutils

#=================================================================
#
#		RAGE Skeletal Model Skeleton export script
#		This model format was meant for use with the RAGE game engine.
#
#		This script exports a Blender Armature object in the following tab-delimited format:
#
# 		boneCount
# 		bone1name	length	restPosX	restPosY	restPosZ	restRotW	restRotX	restRotY	restRotZ	parentIndex
#		bone2name	... etc
#		...
#		boneNname	... etc
#
#		Installation:
#		1) Copy the script into your Blender addons folder
#		2) go into File -> User Preferences -> Add-ons -> Install from File
#		3) Navigate to where you pasted the script and select it.
#		4) Enable the script from the Addons menu.
#		5) Save user preferences.

#
#		Usage:
#		Simply select an Armature object
#		Then select: File -> Export -> "RAGE Skeletal Model Skeleton (.rks)"
#		Choose an output directory and filename
#		Click "Export Skeleton"
#
#		Author: Luis Gutierrez
#
#=================================================================

bl_info = {
	"name": "RAGE Skeletal Model Skeleton (.rks)",
	"author": "Luis Gutierrez",
	"version": (1, 0),
	"blender": (2, 6, 4),
	"location": "File > Import-Export > RKS",
	"description": "Export RAGE Skeletal Skeleton File",
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



def export_rage_skeleton(context, filepath):
	print("=============== Starting Export Script ===============")

	# User must first select an Armature Object before executing this script
	if bpy.context.object.type != 'ARMATURE':
		print("ERROR: An Armature object must be selected to export a skeleton")
		return {'FINISHED'}

	# Open the file for writing
	f = open(filepath,'w')
	sce = bpy.context.scene
	ob = bpy.context.object

	# Get the Armature object
	armature = ob.data

	# Backup the current armature position
	initial_pose_position = armature.pose_position

	# Make the bone be in rest position
	armature.pose_position = 'REST'

	# Keeping an array of tuples containing all required bone data
	bone_data_to_export = []

	bone_names = []
	# Get a list of all bone names
	for bone in armature.bones:
		bone_names.append(bone.name)

	# Iterate through each bone, adding its data to an array of export data
	for bone in armature.bones:

		# Getting the bone's name:
		b_name = bone.name

		# Getting the bone's length:
		b_len = bone.length

		# Getting the bone's rest position relative to parent as a 3D Vector:
		# Translate the bone's head's position by the mat_world
		b_pos = mat_world * bone.head


		# Getting the bone's rest rotation relative to parent as a Quaternion:
		b_rot = bone.matrix.to_quaternion()

		# Getting the bone's parent's index
			# NOTE: this index is dependent upon the order of the bones in armature.bones array.
			# If any modification is done to an Armature object in Blender that changes the ordering of the armature.bones array,
			# all mesh, skeleton, and animation files that use this Armature object will have to be re-exported.
		if bone.parent is not None:
			b_parent = bone_names.index(bone.parent.name)
		else:
			# We choose -1 as a null value for parent bone index. i.e.: no parent bone
			b_parent = -1
			# If the bone has no parent, rotate it to align with RAGE's coordinate system
			b_rot = (mat_world * bone.matrix).to_quaternion()


		cur_bone_data = (b_name, b_len, b_pos[0], b_pos[1], b_pos[2], b_rot[0], b_rot[1], b_rot[2], b_rot[3], b_parent)

		# Adding this bone data to our export data array
		bone_data_to_export.append(	cur_bone_data )

	# Restoring the previous pose position
	armature.pose_position = initial_pose_position

	#=================================================================
	# Exporting all of the bone data
	#=================================================================

	# First export the number of bones in the skeleton:
	f.write("%d\n" % (len(bone_names)))

	# Iterate through all of the bone data and export it all
	for bone in bone_data_to_export:
		# Writing to file line as tab-delimited values
		# map converts each element in bone to a string
		f.write( "\t".join(map(str,bone)) + "\n")
	print("Successfully exported %d bone%s" % (len(bone_names), "s" if (len(bone_names) != 1) else "" ))
	print("File saved to \"%s\"" % filepath)
	return{'FINISHED'}

#ExportHelper is a helper class, defines filename and invoke() function which calls
# file selector.
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

class ExportRAGESkeleton(Operator, ExportHelper):
	"""This is the tooltip of the operator and in generated docs"""
	bl_idname = "export_rage_skeletal.skeleton" #this is how bpy.ops_import_test.some_data is constructed
	bl_label = "Export Skeleton"

	#ExportHelper mixin class uses this
	#For skeleton file
	filename_ext = ".rks"

	filter_glob = StringProperty(
		default="*.rks",
		options={'HIDDEN'},
		maxlen=255, #Maxinternal buffer length, longer would be clamped.
		)

	def execute(self, context):
		return export_rage_skeleton(context, self.filepath)


#Only needed if you want to add into dynamic menu
def menu_func_export(self,context):
	self.layout.operator(ExportRAGESkeleton.bl_idname, text="RAGE Skeletal Model Skeleton (.rks)")

def register():
	bpy.utils.register_class(ExportRAGESkeleton)
	bpy.types.INFO_MT_file_export.append(menu_func_export)

def unregister():
	bpy.utils.unregister_class(ExportRAGESkeleton)
	bpy.types.INFO_MT_file_export.remove(menu_func_export)

if __name__ == "__main__":
	register()

	#Uncomment this line to actually run the exporter on script execution
	bpy.ops.export_test.export_rage_skeletal.skeleton('INVOKE_DEFAULT')
