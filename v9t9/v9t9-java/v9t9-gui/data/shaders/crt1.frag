uniform sampler2D canvasTexture;
uniform sampler2D pixelTexture;
uniform ivec2 visible;
uniform ivec2 viewport;

void main()
{
    vec4 color = texture2D(canvasTexture, gl_TexCoord[0].st);
    vec4 pixel = texture2D(pixelTexture, gl_TexCoord[1].st);
    if (viewport.x >= visible.x) {
	    gl_FragColor = color * pixel * vec4(1.5, 1.5, 1.5, 1);
    } else {
	    gl_FragColor = color;
	}    
}