#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

#ifndef GENERATE_SLICES
    #define sLayer sliceLayer
    #define wPos wPosition
    #define lPos lPosition
    #define wNorm wNormal
    #define wTan wTangent
    #define color Color
    #define tCoord texCoord
    #define tCoord2 texCoord2
    #define vColors vertColors
    #define fogdist fogDistance
#endif

uniform vec4 m_BaseColor;
uniform vec4 g_AmbientLightColor;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;
attribute vec4 inTangent;
#ifndef GENERATE_SLICES
    attribute float inTexCoord8;
    uniform float m_StackHeight;
    out float sLayer;
#endif

#ifdef SEPARATE_TEXCOORD
    attribute vec2 inTexCoord2;
#endif
#if defined (VERTEX_COLOR) || defined(USE_VERTEX_COLORS_AS_SUN_INTENSITY)
    attribute vec4 inColor;
#endif
#ifdef USE_FOG
    uniform vec3 g_CameraPosition;
#endif

out vec3 wPos;
out vec3 lPos;
out vec3 wNorm;
out vec4 wTan;
out vec4 color;
out vec2 tCoord;
#ifdef SEPARATE_TEXCOORD
    out vec2 tCoord2;
#endif
#ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
    out vec4 vColors;
#endif
#ifdef USE_FOG
    out float fogdist;
#endif

void main(){

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;
    vec3 modelSpaceTan  = inTangent.xyz;

    lPos = modelSpacePos.xyz;

    #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
        vColors = inColor;
    #endif

    #ifdef NUM_MORPH_TARGETS
        #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
            Morph_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
            Morph_Compute(modelSpacePos, modelSpaceNorm);
        #endif
    #endif

    #ifdef NUM_BONES
         #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
            Skinning_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
         #else
            Skinning_Compute(modelSpacePos, modelSpaceNorm);
         #endif
    #endif

    tCoord = inTexCoord;
    #ifdef SEPARATE_TEXCOORD
        tCoord2 = inTexCoord2;
    #endif

    vec4 worldPos = TransformWorld(modelSpacePos);
    wNorm = TransformWorldNormal(modelSpaceNorm);
    wTan = vec4(TransformWorldNormal(modelSpaceTan), inTangent.w);
    color = m_BaseColor;

    #ifndef GENERATE_SLICES
        sLayer = inTexCoord8;
        worldPos.xyz += normalize(wNorm) * sLayer * m_StackHeight;
        if (sLayer <= 0.0) {
            sLayer = -1.0;
        }
    #endif

    gl_Position = g_ViewProjectionMatrix * worldPos;
    wPos = worldPos.xyz;

    #ifdef VERTEX_COLOR
        color *= inColor;
    #endif
    #ifdef USE_FOG
        fogdist = distance(g_CameraPosition, TransformWorld(modelSpacePos).xyz);
    #endif

}