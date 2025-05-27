
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "RenthylPlus/ShaderLib/Projection.glsllib"
#import "RenthylPlus/ShaderLib/Shadows.glsllib"

uniform sampler2D m_SceneDepthMap;
uniform sampler2D m_ShadowMap;
uniform mat4 m_CamViewProjectionInverse;
uniform mat4 m_LightViewProjectionMatrix;
uniform int m_LightIndex;
uniform int m_LightType;
uniform vec2 m_LightRange;

#ifdef NORMALS
    uniform sampler2D m_SceneNormalsMap;
    uniform vec3 m_LightPosition;
#endif

varying vec2 texCoord;

void setExposed() {
    gl_FragColor.r = 1 << m_LightIndex;
}

void main() {
    
    float sceneDepth = texture2D(m_SceneDepthMap, texCoord).r;
    if (sceneDepth >= 1.0) {
        setExposed();
        return;
    }

    vec3 worldPos = getPosition(texCoord, sceneDepth, m_CamViewProjectionInverse);

    #ifdef NORMALS
        // triangles facing away from the light source are gauranteed to be in shadow
        vec3 normal = texture2D(m_SceneNormalsMap, texCoord).xyz;
        vec3 lightDir;
        if (m_LightType == 0) {
            lightDir = m_LightPosition;
        } else {
            lightDir = normalize(worldPos - m_LightPosition);
        }
        if (dot(normal, lightDir) > 0.0) {
            discard;
        }
    #endif

    vec4 lightViewPos = m_LightViewProjectionMatrix * vec4(worldPos, 1.0);
    vec3 lightUv = ((lightViewPos.xyz / lightViewPos.w) + 1.0) * 0.5;
    float dist = lightUv.z;
    float shadow = textureLod(m_ShadowMap, lightUv.xy, 0.0).r;
    if (dist >= 0.0 && dist <= 1.0) {
        if (lightUv.x >= 0.0 && lightUv.x <= 1.0 && lightUv.y >= 0.0 && lightUv.y <= 1.0) {
            //shadow = mix(m_LightRange.x, m_LightRange.y, shadow);
            if (dist - 0.001 <= shadow) {
                setExposed();
            }
        } else if (m_LightType == 0) {
            setExposed();
        }
    } else if (shadow >= 1.0) {
        setExposed();
    }
    
}











