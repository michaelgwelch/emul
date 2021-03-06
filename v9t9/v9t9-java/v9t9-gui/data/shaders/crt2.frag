uniform sampler2D canvasTexture;
uniform sampler2D pixelTexture;
uniform ivec2 visible;
uniform ivec2 viewport;

void main()
{
    vec4 color = texture2D(canvasTexture, gl_TexCoord[0].st);
    if (viewport.x >= visible.x * 2) {
        vec4 mon = texture2D(pixelTexture, gl_TexCoord[1].st);
	    if (visible.x > 256) {
	        // 512-pixel mode
	        float fx = float(visible.x);
	        vec4 color2 = texture2D(canvasTexture, vec2(gl_TexCoord[0].s + 1.0 / fx, gl_TexCoord[0].t));
	        // brighten
	        color = (color * vec4(2, 2, 2, 2) + color2) / vec4(3, 3, 3, 3);
	    	gl_FragColor = (color + color + (color * mon)) / vec4(2, 2, 2, 1);
	    }
		else {
	    	gl_FragColor = (color * vec4(.75, .75, .75, 1)) + (color + vec4(.1, .1, .1, 0)) * mon * vec4(0.9, 0.9, 0.9, 1);
	    }
    } else {
  	   gl_FragColor = color;
    }
    
    
}