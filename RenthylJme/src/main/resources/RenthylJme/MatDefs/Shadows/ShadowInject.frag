
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
    uint mask = uint(texture2D(m_LightContribution, texCoord).r);
    float shadowIntensity = clamp((clamp(m_Intensity, 0.0, 1.0) * (m_NumLights - bitCount(mask))) / m_NumLights, 0.0, 1.0);
    gl_FragColor = mix(color, m_ShadowColor, shadowIntensity);

}
