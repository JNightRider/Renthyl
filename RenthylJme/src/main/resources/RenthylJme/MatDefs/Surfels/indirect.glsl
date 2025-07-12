
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = LOCAL_SIZE_Z) in;

uniform sampler2D DepthMap;
uniform mat4 ViewProjectionMatrix;
uniform int NumSurfels;

layout (std430) buffer Surfels {
    float data[];
};

layout (RGBA8) uniform image2D IndirectMap;

const uint surfelComponents = 8u;

float fastSqrt(float n) {
    return n * inversesqrt(n);
}

void main() {

    uint i = gl_LocalInvocationIndex * surfelComponents;

    // calculate surfel clip space
    vec3 position = vec3(data[i], data[i + 1], data[i + 2]);
    vec4 clip = ViewProjectionMatrix * vec4(position, 1.0);
    clip.xyz /= clip.w;
    ivec2 viewPort = imageSize(IndirectMap);
    ivec2 baseTexel = ivec2((clip.xy + vec2(1.0)) * 0.5 * viewPort);
    if (clip.z > texelFetch(DepthMap, baseTexel).r) {
        return;
    }
    ivec2 invTexel = baseTexel + ivec2(gl_LocalInvocationID.xy);

    //vec3 normal = vec3(data[i + 3], data[i + 4], 0.0);
    //normal.z = sqrt(1 - normal.x*normal.x - normal.y*normal.y);

    vec3 color = vec3(data[i + 5], data[i + 6], data[i + 7]);
    imageStore(IndirectMap, invTexel, vec4(color, 1.0));

}
