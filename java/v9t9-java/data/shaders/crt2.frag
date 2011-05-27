uniform sampler2D canvasTexture;
uniform sampler2D pixelTexture;
uniform ivec2 visible;
uniform ivec2 viewport;

void main()
{
    vec4 color = texture2D(canvasTexture, gl_TexCoord[0].st);
    if (visible.x > 256) {
        // 512-pixel mode
        vec4 color2 = texture2D(canvasTexture, vec2(gl_TexCoord[0].s + 1.0f / visible.x, gl_TexCoord[0].t));
        color = (color * 3 + color2) / vec4(4, 4, 4, 4);
    }
    vec4 mon = texture2D(pixelTexture, gl_TexCoord[1].st);
    
    gl_FragColor = (color * vec4(.5, .5, .5, 1)) + (color + vec4(.1, .1, .1, 0)) * mon * vec4(0.9, 0.9, 0.9, 1);
    //gl_FragColor = (color * vec4(.5, .5, .5, 1)) + (color * mon);
    
    //    gl_FragColor = (color + color * mon)  * mon ;
    //gl_FragColor = (color + color * mon) / 2 + mon / 8;
    
    //vec4 comb = max(color * mon, mon / 2) * mon;
    
    //gl_FragColor = comb;
    
}