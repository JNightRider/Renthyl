
layout (local_size_x = LOCAL_SIZE_X, local_size_y = LOCAL_SIZE_Y, local_size_z = 1) in;

#import "RenthylJme/ShaderLib/Projection.glsllib"
#import "RenthylJme/ShaderLib/Shadows.glsllib"

layout (R32F) uniform image2D Contribution;
uniform sampler2D SceneDepthMap;
uniform sampler2D ShadowMap;
uniform mat4 CamViewProjectionInverse;
uniform mat4 LightViewProjectionMatrix;
uniform int LightIndex;
uniform int LightType;
uniform bool Overwrite;

#ifdef NORMALS
    uniform sampler2D SceneNormalsMap;
    uniform vec3 LightPosition;

    vec3 readNormals(in sampler2D normals, inout ivec2 texel) {
        #ifndef READ_NORMALS_LAMBDA
            #define READ_NORMALS_LAMBDA return texelFetch(normals, texel, 0).xyz;
        #endif
        READ_NORMALS_LAMBDA
    }
#endif

uint evaluateShadow(inout ivec2 texel) {
    float depth = texelFetch(SceneDepthMap, texel, 0).r;
    if (depth >= 1.0) { // beyond light range
        return 1 << LightIndex;
    }
    vec2 texCoord = vec2(texel) / (gl_NumWorkGroups.xy * gl_WorkGroupSize.xy);
    vec3 worldPos = getPosition(texCoord, depth, CamViewProjectionInverse);
    #ifdef NORMALS
        vec3 normal = readNormals(SceneNormalsMap, texel);
        if (dot(normal, LightType == 0 ? LightPosition : normalize(worldPos - LightPosition)) > 0.0) {
            return 0; // normal facing away from light source
        }
    #endif
    vec4 lightViewPos = LightViewProjectionMatrix * vec4(worldPos, 1.0);
    vec3 lightUv = ((lightViewPos.xyz / lightViewPos.w) + 1.0) * 0.5;
    float dist = lightUv.z;
    float shadow = textureLod(ShadowMap, lightUv.xy, 0.0).r;
    bool withinLightDist = dist >= 0.0 && dist <= 1.0;
    bool withinLightScreen = lightUv.x >= 0.0 && lightUv.x <= 1.0 && lightUv.y >= 0.0 && lightUv.y <= 1.0;
    if ((withinLightDist && ((withinLightScreen && dist - 0.001 <= shadow) || (!withinLightScreen && LightType == 0))) || (!withinLightDist && shadow >= 1.0)) {
        return 1 << LightIndex;
    }
    return 0;
}

void main() {
    ivec2 texel = ivec2(gl_GlobalInvocationID.xy);
    uint result = Overwrite ? 0 : floatBitsToUint(imageLoad(Contribution, texel).x);
    result |= evaluateShadow(texel);
    imageStore(Contribution, texel, vec4(uintBitsToFloat(result), 0.0, 0.0, 0.0));
}











