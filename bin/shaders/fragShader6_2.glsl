#version 430

in vec2 texCoord; // Tọa độ texture từ vertex shader
out vec4 fragColor;

uniform sampler2D tex; // Texture sampler

void main(void) {
    fragColor = texture(tex, texCoord); // Lấy màu từ texture dựa vào tọa độ
}
