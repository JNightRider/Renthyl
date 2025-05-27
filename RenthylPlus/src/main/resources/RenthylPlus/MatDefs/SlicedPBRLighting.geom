
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

layout (triangles) in;
#ifdef GENERATE_SLICES
    layout (triangle_strip, max_vertices = (NUM_SLICES + 1) * 3) out;
#else
    layout (triangle_strip, max_vertices = 3) out;
#endif

uniform vec3 g_CameraPosition;
uniform int m_NumSlices;
uniform float m_StackHeight;

#ifndef GENERATE_SLICES
    in float sLayer[];
#endif
in vec3 wPos[];
in vec3 lPos[];
in vec3 wNorm[];
in vec4 wTan[];
in vec4 color[];
in vec2 tCoord[];
#ifdef SEPARATE_TEXCOORD
    in vec2 tCoord2[];
#endif
#ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
    in vec4 vColors[];
#endif
#ifdef USE_FOG
    in float fogdist[];
#endif

out float sliceLayer;
out vec3 wPosition;
out vec3 lPosition;
out vec3 wNormal;
out vec4 wTangent;
out vec2 texCoord;
out vec4 Color;
#ifdef SEPARATE_TEXCOORD
    out vec2 texCoord2;
#endif
#ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
    out vec4 vertColors;
#endif
#ifdef USE_FOG
    out float fogDistance;
#endif

#ifdef GENERATE_SLICES
    float distSqr(vec3 v1, vec3 v2) {
        v1 -= v2;
        return v1.x * v1.x + v1.y * v1.y;
    }

    float getFaceDistanceSqr() {
        return min(min(distSqr(wPos[0], g_CameraPosition), distSqr(wPos[1], g_CameraPosition)), distSqr(wPos[2], g_CameraPosition));
    }

    vec3 getFaceNormal() {
        return (wNorm[0] + wNorm[1] + wNorm[2]) * 0.3333;
    }

    float mapRange(float value, float fromMin, float fromMax) {
        return (value - fromMin) / (fromMax - fromMin);
    }

    void createVertex(int n, int i, int slices, float height) {
        sliceLayer = slices != 0 ? float(i) / slices : 0.0;
        wNormal = wNorm[n];
        wPosition = wPos[n] + wNormal * (sliceLayer * height + m_StackHeight - height);
        if (i == 0) {
            sliceLayer = -1.0;
        }
        gl_Position = g_ViewProjectionMatrix * vec4(wPosition, 1.0);
        lPosition = lPos[n];
        wTangent = wTan[n];
        texCoord = tCoord[n];
        Color = color[n];
        #ifdef SEPARATE_TEXCOORD
            texCoord2 = tCoord2[n];
        #endif
        #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
            vertColors = vColors[n];
        #endif
        #ifdef USE_FOG
            fogDistance = fogdist[n];
        #endif
        EmitVertex();
    }
#endif

void main() {

    #ifdef GENERATE_SLICES
        float d = pow(clamp(mapRange(sqrt(getFaceDistanceSqr()), 100.0, 30.0), 0.0, 1.0), 2.0);
        int slices = int(d * m_NumSlices);
        float height = m_StackHeight * d;
        // spawn higher layers first to reduce overdraw
        for (int i = slices; i >= 0; i--) {
            createVertex(0, i, slices, height);
            createVertex(1, i, slices, height);
            createVertex(2, i, slices, height);
            EndPrimitive();
        }
    #else
        for (int i = 0; i < 3; i++) {
            gl_Position = gl_in[i].gl_Position;
            sliceLayer = sLayer[i];
            wPosition = wPos[i];
            lPosition = lPos[i];
            wNormal = wNorm[i];
            wTangent = wTan[i];
            texCoord = tCoord[i];
            Color = color[i];
            #ifdef SEPARATE_TEXCOORD
                texCoord2 = tCoord2[i];
            #endif
            #ifdef USE_VERTEX_COLORS_AS_SUN_INTENSITY
                vertColors = vColors[i];
            #endif
            #ifdef USE_FOG
                fogDistance = fogdist[i];
            #endif
            EmitVertex();
        }
        EndPrimitive();
    #endif

}
