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
#   	RAGE Skeletal Model Animation export script
#
#		This script exports the active Armature's active Action in the following tab delimited file:
#		frameCount is N, bone count is M
#
#		boneCount	frameCount
#		frame0bone0LocX	frame0bone0LocY	frame0bone0LocZ	frame0bone0RotW	frame0bone0RotX	frame0bone0RotY	frame0bone0RotZ	frame0bone0ScaX	frame0bone0ScaY	frame0bone0ScaZ
#		frame0bone1LocX	... etc
#		...
#		frame0boneMLocX	... etc
#		frame1bone1LocX	... etc
#		...
#		frameNbone0LocX	... etc
#		...
#		frameNboneMLocX	...	etc
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
#		Then select: File -> Export -> "RAGE Skeletal Model Animation (.rka)"
#		Choose an output directory and filename
#		Click "Export Animation"
#
#		Author: Luis Gutierrez
#
#=================================================================

bl_info = {
	"name": "RAGE Skeletal Model Animation (.rka)",
	"author": "Luis Gutierrez",
	"version": (1, 0),
	"blender": (2, 6, 4),
	"location": "File > Import-Export > RKA",
	"description": "Export RAGE Skeletal Animation File",
	"warning": "",
	"wiki_url": "",
	"tracker_url": "",
	"support": "COMMUNITY",
	"category": "Import-Export"
}

def export_rage_animation(context, filepath):
	print("=============== Starting script ===============")

	# User must first select an Armature Object before executing this script
	if bpy.context.object.type != 'ARMATURE':
		print("ERROR: An armature must be selected to export an animation")
		return {'FINISHED'}

	sce = bpy.context.scene
	ob = bpy.context.object

	# The selected Armature must have animation data
	if ob.animation_data is None:
		print("ERROR: The selected Armature has no Animation data")
		print("\tCreate an action and try again.")
		return {'FINISHED'}

	# The selected Armature must have an active Action
	if ob.animation_data.action is None:
		print("ERROR: The selected Armature has no active Action")
		print("\tSelect an action and try again.")
		return {'FINISHED'}

	# Get the Armature object
	armature = ob.data

	# This gets the action name, needed for finding length of animation
	current_action_name = ob.animation_data.action.name


	# Open the file for writing
	f = open(filepath,'w')

	# This gets the action name, needed for finding length of animation
	current_action_name = ob.animation_data.action.name

	# To find the length of the current animation:
	# iterate through all keyframes and find the frame number of the last keyframe
	last_frame_in_anim = 0
	current_action = bpy.data.actions[current_action_name]
	for fcurve in current_action.fcurves:
		for keyframe in fcurve.keyframe_points:
			# Each channel keyframe has a tuple that resembles (frameNo, value)
			if int(keyframe.co[0]) > last_frame_in_anim:
				last_frame_in_anim = int(keyframe.co[0])
	anim_length = last_frame_in_anim + 1

	#Printing animation name and length
	print("%s: %i frames" % (current_action_name , anim_length))


	bone_names = []
	# Get a list of all bone names
	for bone in armature.bones:
		bone_names.append(bone.name)



	sorted_bone_indices = []
	temp_arr = []
	for i in range(0,len(ob.pose.bones)):
		temp_arr.append([ob.pose.bones[i].name,i])
	temp_arr.sort()
	for b in temp_arr:
		sorted_bone_indices.append(b[1])
	# This constructs a list of bone indices in alphabetical order as they relate to the bone_names

	animation_data = []

	for frame in range(0,anim_length):
		sce.frame_set(frame)
		sce.update()
		frame_data = []
		#Go through the list of sorted bone name indices
		for bname in bone_names:
			b = ob.pose.bones[bname]

			# Decomposing the bone's local transformation relative to its parent
			bloc, brot, bsca = b.matrix_basis.decompose()

			#print("\t%s, frame %d : loc: (%f,%f,%f),  rot(%f,%f,%f,%f),  scale(%f,%f,%f)" % (bname, frame,bloc[0], bloc[1], bloc[2], brot[0], brot[1], brot[2], brot[3], bsca[0], bsca[1], bsca[2]))

			# Adding this to the frame_data as a tuple of the values, flipping y and z values
			frame_data.append( (bloc[0], bloc[1], bloc[2], brot[0], brot[1], brot[2], brot[3], bsca[0], bsca[1], bsca[2]) )

		# Adding this frame's data to the animation data
		animation_data.append(frame_data)


	#===============================================================
	# Writing the data to the animation file
	#===============================================================
	# Writing the number of bones and number of frames to the file
	# For compatability checking
	header_data = [len(bone_names), anim_length]
	f.write( "\t".join( map(str, header_data)) + "\n")

	# Iterate through each frame
	for frame in animation_data:
		# Iterate through each bone
		for bone in frame:
			# Writing to file line as tab-delimited values
			# map converts each element in bone to a string
			f.write( "\t".join( map(str, bone) ) + "\n" )

	f.close()
	sce.frame_set(0)
	print("File saved to \"%s\"" % filepath)
	return{'FINISHED'}







#ExportHelper is a helper class, defines filename and invoke() function which calls
# file selector.
from bpy_extras.io_utils import ExportHelper
from bpy.props import StringProperty, BoolProperty, EnumProperty
from bpy.types import Operator

class ExportRAGEAnimation(Operator, ExportHelper):
	"""This is the tooltip of the operator and in generated docs"""
	bl_idname = "export_rage_skeletal.animation" #this is how bpy.ops_import_test.some_data is constructed
	bl_label = "Export Animation"

	#ExportHelper mixin class uses this
	#For animation file
	filename_ext = ".rka"

	filter_glob = StringProperty(
		default="*.rka",
		options={'HIDDEN'},
		maxlen=255, #Maxinternal buffer length, longer would be clamped.
		)
	def execute(self, context):
		return export_rage_animation(context, self.filepath)


#Only needed if you want to add into dynamic menu
def menu_func_export(self,context):
	self.layout.operator(ExportRAGEAnimation.bl_idname, text="RAGE Skeletal Model Animation (.rka)")

def register():
	bpy.utils.register_class(ExportRAGEAnimation)
	bpy.types.INFO_MT_file_export.append(menu_func_export)

def unregister():
	bpy.utils.unregister_class(ExportRAGEAnimation)
	bpy.types.INFO_MT_file_export.remove(menu_func_export)

if __name__ == "__main__":
	register()

	#Uncomment this line to actually run the exporter on script execution
	bpy.ops.export_rage_skeletal.animation('INVOKE_DEFAULT')
