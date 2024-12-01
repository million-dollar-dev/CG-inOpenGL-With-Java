#version 330 core
out vec4 FragColor;

uniform sampler2D reflectionTexture; // Texture phản chiếu nếu có
uniform vec3 waterColor; // Màu mặt nước

void main() {
    // Màu đơn giản hoặc kết hợp phản chiếu nhẹ
    vec4 reflection = texture(reflectionTexture, gl_FragCoord.xy / 800.0); // Kích thước viewport giả định
    FragColor = mix(vec4(waterColor, 1.0), reflection, 0.2); // 20% phản chiếu
}
