
uniform mat4 g_ViewProjectionMatrix;
uniform int m_NumSlices;
uniform float m_StackHeight;

layout (triangle) in;
layout (triangle, max_vertices = m_NumSlices * 3) out;

in vec3 wPosition[];
in vec3 wNormal[];
in vec4 wTangent[];
in vec2 texCoord[];
#ifdef SEPARATE_TEXCOORD
    in vec2 texCoord2[];
#endif
#ifdef USE_FOG
    in float fogDistance[];
#endif

out GS_OUT {
    float sliceLayer;
    bool protectedLayer;
    vec3 wPosition;
    vec3 wNormal;
    vec4 wTangent;
    vec2 texCoord;
    #ifdef SEPARATE_TEXCOORD
        vec2 texCoord2;
    #endif
    #ifdef USE_FOG
        float fogDistance;
    #endif
} gsout;

void createVertex(int n, int i) {
    gsout.sliceLayer = float(i) / m_NumSlices;
    gsout.protectedLayer = (i == 0);
    gsout.wPosition = wPosition[n] + wNormal[n] * gsout.sliceLayer * m_StackHeight;
    gl_Position = g_ViewProjectionMatrix * vec4(gsout.wPosition, 1.0);
    gsout.wNormal = wNormal[n];
    gsout.wTangent = wTangent[n];
    gsout.texCoord = texCoord[n];
    #ifdef SEPARATE_TEXCOORD
        gsout.texCoord2 = texCoord2[n];
    #endif
    #ifdef USE_FOG
        gsout.fogDistance = fogDistance[n];
    #endif
    emitVertex();
}

void main() {

    for (int i = 0; i < m_NumSlices; i++) {
        CreateVertex(0, i);
        CreateVertex(1, i);
        CreateVertex(2, i);
        EndPrimitive();
    }

}
