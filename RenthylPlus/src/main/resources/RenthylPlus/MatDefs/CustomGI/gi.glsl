
#extension GL_NV_atomic_float enable

// local space evaluates space around the group's pixel
// z size is not utilized yet
layout (local_size_x = 16, local_size_y = 16, local_size_z = 1) in;

#import "RenthylPlus/ShaderLib/Projection.glsllib"

uniform sampler2D Diffuse;
uniform sampler2D Specular;
uniform sampler2D Normals;
uniform sampler2D Depth;
uniform sampler2D LightColor;
uniform mat4 ViewProjectionInverse;

uniform float DiffuseRange; // determines how diffuse reflections carry over distance

layout (RGBA8) uniform image2D IndirectLight;

shared float indirectR;
shared float indirectG;
shared float indirectB;

struct Surface {
    vec3 color;
    vec3 light;
    vec3 position;
    vec3 normal;
    vec3 specular;
    float depth;
};

Surface readSurface(inout vec2 texCoord, inout ivec2 offset) {
    Surface surface;
    surface.color = textureLodOffset(Diffuse, texCoord, 0.0, offset).rgb;
    surface.light = textureLodOffset(LightColor, texCoord, 0.0, offset).rgb;
    surface.normal = normalize(textureLodOffset(Normals, texCoord, 0.0, offset).xyz);
    surface.specular = normalize(textureLodOffset(Specular, texCoord, 0.0, offset).xyz);
    surface.depth = textureLodOffset(Depth, texCoord, 0.0, offset).r;
    surface.position = getPosition(texCoord + vec2(offset) / gl_NumWorkGroups.xy, surface.depth, ViewProjectionInverse);
    return surface;
}

bool isBeyondSurfacePlane(inout Surface surface, in vec3 point) {
    point = normalize(point - surface.position);
    return dot(point, surface.normal) > 0.0;
}

float mapRange(inout float value, inout float minHandle, inout float maxHandle) {
    return (value - minHandle) / (maxHandle - minHandle);
}

void main() {

    ivec2 texel = ivec2(gl_WorkGroupID.xy);
    vec2 texCoord = vec2(texel) / gl_NumWorkGroups.xy;
    Surface subject = readSurface(texCoord, ivec2(0.0));
    Surface surface = readSurface(texCoord, gl_LocalInvocationID.xy);

    // direction from surface to subject
    vec3 surfaceDir = normalize(subject.position - surface.position);
    float surfaceDist = distance(subject.position, surface.position);

    // surface does not influence subject if the normals are pointing in generally the same direction relative to the positions
    vec3 indirect = vec3(0.0);
    if (isBeyondSurfacePlane(surface, subject.position) && isBeyondSurfacePlane(subject, surface.position)) {
        // does not account for changes in depth between surface and subject
        vec3 specularColor = surface.light * clamp(dot(surfaceDir, surface.specular), 0.0, 1.0);
        vec3 diffuseColor = surface.color * mapRange(surfaceDist, DiffuseRange, 0.0);
        indirect += specularColor + diffuseColor;
    }

    atomicAdd(indirectR, indirect.r);
    atomicAdd(indirectG, indirect.g);
    atomicAdd(indirectB, indirect.b);

    if (gl_LocalInvocationIndex == 0) {
        groupMemoryBarrier();
        imageStore(IndirectLight, texel, vec4(indirectR, indirectG, indirectB, 1.0));
    }

}
