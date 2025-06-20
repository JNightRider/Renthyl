
layout (local_size_x = LOCAL_X, local_size_y = LOCAL_Y, local_size_z = LOCAL_z) in;

layout(RGBA8) uniform image3D VoxelMap;
uniform vec4 Value;

void main() {
    
    imageStore(VoxelMap, ivec3(gl_GlobalInvocationID), Value);
    
}

