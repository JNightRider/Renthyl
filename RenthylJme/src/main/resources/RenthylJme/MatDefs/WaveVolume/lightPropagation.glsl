
#ifndef LOCAL_SIZE
    #define LOCAL_SIZE 1
#endif
layout (local_size_x = LOCAL_SIZE, local_size_y = LOCAL_SIZE, local_size_z = LOCAL_SIZE) in;

layout (RGBA32F) uniform image3D Volume;
uniform vec3 Position;
uniform vec3 UnitSize;
uniform float Tension;
uniform float Damping;

#define NUM_SAMPLES 26
const ivec3 sampleOffsets[] = {
    ivec3(-1, -1, -1),
    ivec3(0, -1, -1),
    ivec3(1, -1, -1),
    ivec3(-1, 0, -1),
    ivec3(0, 0, -1),
    ivec3(1, 0, -1),
    ivec3(-1, 1, -1),
    ivec3(0, 1, -1),
    ivec3(1, 1, -1),
    ivec3(-1, -1, 0),
    ivec3(0, -1, 0),
    ivec3(1, -1, 0),
    ivec3(-1, 0, 0),
    //ivec3(0, 0, 0),
    ivec3(1, 0, 0),
    ivec3(-1, 1, 0),
    ivec3(0, 1, 0),
    ivec3(1, 1, 0),
    ivec3(-1, -1, 1),
    ivec3(0, -1, 1),
    ivec3(1, -1, 1),
    ivec3(-1, 0, 1),
    ivec3(0, 0, 1),
    ivec3(1, 0, 1),
    ivec3(-1, 1, 1),
    ivec3(0, 1, 1),
    ivec3(1, 1, 1),
};

struct Particle {
    ivec3 index;
    vec3 position;
    vec3 offset;
    vec3 velocity;
    float wavelength;
};

Particle readParticleFromVolume(inout ivec3 index) {
    Particle p;
    p.index = index;
    vec4 data = imageLoad(Volume, index);
    p.offset.xy = unpackHalf2x16(floatBitsToUint(data.x));
    vec2 y = unpackHalf2x16(floatBitsToUint(data.y));
    p.offset.z = y.x;
    p.velocity.x = y.y;
    p.velocity.yz = unpackHalf2x16(floatBitsToUint(data.z));
    p.wavelength = data.w;
    p.position = Position + UnitSize * index + p.offset;
    return p;
}

void writeParticleToVolume(inout Particle p) {
    vec4 data;
    data.x = uintBitsToFloat(packHalf2x16(p.offset.xy));
    data.y = uintBitsToFloat(packHalf2x16(vec2(p.offset.z, p.velocity.x)));
    data.z = uintBitsToFloat(packHalf2x16(p.velocity.yz));
    data.w = p.wavelength;
    imageStore(Volume, p.index, data);
}

vec3 negate(inout vec3 v) {
    v.x = -v.x;
    v.y = -v.y;
    v.z = -v.z;
    return v;
}

void main() {

    Particle current = readParticleFromVolume(gl_GlobalInvocationID);
    float accumWavelength = 0f;

    for (uint i = 0; i < NUM_SAMPLES; i++) {
        ivec3 offset = sampleOffsets[i];
        Particle p = readParticleFromVolume(offset + current.index);
        vec3 neutral = negate(UnitSize * offset);
        vec3 force = (current.position - p.position - neutral) * -Tension;
        current.velocity += force;
        accumWavelength += p.wavelength;
    }

    current.position += current.velocity;
    current.wavelength = accumWavelength / NUM_SAMPLES;
    writeParticleToVolume(current);

}
