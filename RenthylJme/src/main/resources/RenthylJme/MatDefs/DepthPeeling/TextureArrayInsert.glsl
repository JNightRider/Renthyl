
#ifndef LOCAL_SIZE
    #define LOCAL_SIZE 1
#endif
layout (local_size_x = LOCAL_SIZE, local_size_y = LOCAL_SIZE, local_size_z = 1) in;

layout (RGBA8) uniform image2DArray TargetArray;
layout (RGBA8) uniform sampler2D Color;
layout (R8) uniform sampler2D Depth;
uniform int Index;

void main() {

    vec4 color = imageLoad(Color, gl_GlobalInvocationID.xy);
    color.a = imageLoad(Depth, gl_GlobalInvocationID.xy).r;
    imageStore(TargetArray, ivec3(gl_GlobalInvocationID.xy, Index), color);

}
