
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/MultiSample.glsllib"

uniform sampler2D m_Texture;
uniform sampler2D m_LightContribution;
uniform int m_NumLights;
uniform float m_Intensity;
uniform vec4 m_ShadowColor;

varying vec2 texCoord;

void main() {

    vec4 color = texture2D(m_Texture, texCoord);
    float shadowIntensity = clamp(m_Intensity, 0.0, 1.0) / m_NumLights;
    float factor = 0f;
    int mask = int(texture2D(m_LightContribution, texCoord).r);
    for (int i = 0; i < m_NumLights; i++) {
        if (mask == 0 /*|| (mask & (1 << i)) == 0*/) {
            factor += shadowIntensity;
        }
    }

    gl_FragColor = mix(color, m_ShadowColor, factor);

}
