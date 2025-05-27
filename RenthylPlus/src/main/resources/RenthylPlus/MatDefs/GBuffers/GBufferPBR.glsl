
layout (local_size_x = 4, local_size_y = 4, local_size_z = 1) in;

#define GBUFFER_READ 1
#define ENABLE_PBRLightingUtils_computeDirectLightContribution 1
#define ENABLE_PBRLightingUtils_computeProbesContribution 1

#import "Common/Shaderlib/module/pbrlighting/PBRLightingUtils.glsllib"
#import "RenthylPlus/ShaderLib/GBuffers/PBRCompactModel.glsllib"

uniform mat4 ViewProjectionInverse;
uniform vec3 CameraPosition;
uniform vec4 LightData[LIGHT_DATA_LENGTH];

uniform image2D ResultImage;

void main() {

    ivec2 uv = ivec2(gl_GlobalInvocationID.xy);

    // create surface to read gbuffers to
    PBRSurface surface = GBufferRead_createPBRSurface();

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
    result += surface.bakedLightContribution;
    result += surface.directLightContribution;
    result += surface.envLightContribution;
    result += surface.emission;
    result.a = surface.alpha;

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
