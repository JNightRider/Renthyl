
layout (local_size_x = LOCAL_X, local_size_y = LOCAL_Y, local_size_z = 1) in;

#define COMPUTE_SHADER 1
#define GBUFFER_READ 1
#define SRGB 1 // makes light falloff more realistic
#define ENABLE_PBRLightingUtils_computeLightInWorldSpace 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

// compatibility
#define textureCubeLod textureLod
#define mat3_sub mat3
#define wPosition vec3(0.0)
#define wNormal vec3(0.0, 0.0, 1.0)
#define gl_FragColor outputColorForLayer

#ifndef PBRSurface
    struct StdPBRSurface {
        vec3 position;
        vec3 viewDir;
        vec3 geometryNormal;
        vec3 normal;
        bool frontFacing;
        float depth;
        mat3 tbnMat;
        bool hasTangents;
        vec3 albedo;
        float alpha;
        float metallic;
        float roughness;
        vec3 ao;
        vec3 lightMapColor;
        bool hasBasicLightMap;
        float exposure;
        vec3 emission;
        vec3 diffuseColor;
        vec3 specularColor;
        vec3 fZero;
        float NdotV;
        vec3 bakedLightContribution;
        vec3 directLightContribution;
        vec3 envLightContribution;
        float brightestNonGlobalLightStrength;
    };
    #define PBRSurface StdPBRSurface
#endif
#ifndef Light
    struct StdLight {
        vec4 color;
        vec3 position;
        float type;
        float invRadius;
        float spotAngleCos;
        vec3 spotDirection;
        bool ready;
        float NdotL;
        float NdotH;
        float LdotH;
        float HdotV;
        vec3 vector;
        vec3 dir;
        float fallOff;
    };
    #define Light StdLight
#endif

// define functions for specular AA only available to fragment shaders
vec3 dFdx(inout vec3 x) {
    return x;
}
vec3 dFdy(inout vec3 y) {
    return y;
}

#import "Common/ShaderLib/module/pbrlighting/PBRLightingUtils.glsllib"
#import "RenthylPlus/ShaderLib/GBuffers/PBRCompactModel.glsllib"

uniform mat4 ViewProjectionInverse;
uniform vec3 CameraPosition;
uniform vec4 LightData[LIGHT_DATA_LENGTH];

layout (RGBA8) uniform image2D ResultImage;

void main() {

    ivec2 uv = ivec2(gl_GlobalInvocationID.xy);

    // create surface to read gbuffers to
    PBRSurface surface = GBufferRead_createSurface();

    // read gbuffers to the surface
    GBufferRead_readGBuffersToSurface(surface, uv, ViewProjectionInverse, CameraPosition);

    // calculate necessary pre-lighting components
    PBRLightingUtils_calculatePreLightingValues(surface);

    // Calculate direct lights
    for (int i = 0; i < LIGHT_DATA_LENGTH; i += 3) {
        vec4 lightData0 = LightData[i];
        vec4 lightData1 = LightData[i + 1];
        vec4 lightData2 = LightData[i + 2];
        PBRLightingUtils_computeDirectLightContribution(
            lightData0, lightData1, lightData2,
            surface
        );
    }

    // Calculate environment probes
    PBRLightingUtils_computeProbesContribution(surface);

    // Put it all together
    vec4 result = vec4(0.0);
    //result.rgb += surface.bakedLightContribution;
    result.rgb += surface.directLightContribution;
    //result.rgb += surface.envLightContribution;
    //result.rgb += surface.emission;
    //result.a = surface.alpha;

    //if (surface.position.x >= 0.0) {
        //result *= 0.05;
    //}
    //result.rgb = vec3(minDist / 100.0);
    //result.rgb = vec3(1.0);
    //result.rgb = vec3(surface.metallic, 0.0, 0.0);
    //float val = uintBitsToFloat(packHalf2x16(vec2(0.1, 1.0)));
    //result.rgb = vec3(unpackHalf2x16(floatBitsToUint(val)), 1.0);
    //result.rgb = vec3(surface.normal);
    //result.rgb = surface.position;
    //result.rgb = lightColor;

    #ifdef USE_FOG
        result = MaterialFog_calculateFogColor(vec4(result));
    #endif

    //outputs the final value of the selected layer as a color for debug purposes.
    #ifdef DEBUG_VALUES_MODE
        result = PBRLightingUtils_getColorOutputForDebugMode(m_DebugValuesMode, vec4(result), surface);
    #endif

    // write result to image
    imageStore(ResultImage, uv, result);

}
