
    #define EMITTER_PARTICLE2 0.0
    #define EMITTER_RIBBON 1.0
    #define EMITTER_SPLAT 2.0
    #define EMITTER_UBERSPLAT 3.0
    #define HEAD 0.0
    uniform mat4 u_mvp;
    uniform float u_emitter;
    // Shared
    uniform vec4 u_colors[3];
    uniform vec3 u_vertices[4];
    uniform vec3 u_intervals[4];
    uniform float u_lifeSpan;
    uniform float u_columns;
    uniform float u_rows;
    // Particle2
    uniform vec3 u_scaling;
    uniform vec3 u_cameraZ;
    uniform float u_timeMiddle;
    uniform bool u_teamColored;
    uniform bool u_unshaded;
    uniform sampler2D u_lightTexture;
    uniform float u_lightCount;
    uniform float u_lightTextureHeight;
    // Splat and Uber.
    uniform vec3 u_intervalTimes;
    // Vertices
    attribute float a_position;
    // Instances
    attribute vec3 a_p0;
    attribute vec3 a_p1;
    attribute vec3 a_p2;
    attribute vec3 a_p3;
    attribute float a_health;
    attribute vec4 a_color;
    attribute float a_tail;
    attribute vec3 a_leftRightTop;
    varying vec2 v_texcoord;
    varying vec4 v_color;
    float getCell(vec3 interval, float factor) {
      float start = interval[0];
      float end = interval[1];
      float repeat = interval[2];
      float spriteCount = end - start;
      if (spriteCount > 0.0) {
        // Repeating speeds up the sprite animation, which makes it effectively run N times in its interval.
        // E.g. if repeat is 4, the sprite animation will be seen 4 times, and thus also run 4 times as fast.
        // The sprite index is limited to the number of actual sprites.
        return min(start + mod(floor(spriteCount * repeat * factor), spriteCount), u_columns * u_rows - 1.0);
      }
      return 0.0;
    }
    void particle2() {
      float factor = (u_lifeSpan - a_health) / u_lifeSpan;
      int index = 0;
      if (factor < u_timeMiddle) {
        factor = factor / u_timeMiddle;
        index = 0;
      } else {
        factor = (factor - u_timeMiddle) / (1.0 - u_timeMiddle);
        index = 1;
      }
      factor = min(factor, 1.0);
      float scale = mix(u_scaling[index], u_scaling[index + 1], factor);
      vec4 color = mix(u_colors[index], u_colors[index + 1], factor);
      float cell = 0.0;
      if (u_teamColored) {
        cell = a_leftRightTop[0];
      } else {
        vec3 interval;
        if (a_tail == HEAD) {
          interval = u_intervals[index];
        } else {
          interval = u_intervals[index + 2];
        }
        cell = getCell(interval, factor);
      }
      float left = floor(mod(cell, u_columns));
      float top = floor(cell / u_columns);
      float right = left + 1.0;
      float bottom = top + 1.0;
      left /= u_columns;
      right /= u_columns;
      top /= u_rows;
      bottom /= u_rows;
      if (a_position == 0.0) {
        v_texcoord = vec2(right, top);
      } else if (a_position == 1.0) {
        v_texcoord = vec2(left, top);
      } else if (a_position == 2.0) {
        v_texcoord = vec2(left, bottom);
      } else if (a_position == 3.0) {
        v_texcoord = vec2(right, bottom);
      }
      v_color = color;
      
      vec3 lightingNormal;
      vec3 position;
      if (a_tail == HEAD) {
        vec3 vertices[4];
        if(a_p1[0] != 0.0 || a_p1[1] != 0.0) {
          lightingNormal = vec3(0.0, 0.0, 1.0);
          vec3 vx;
          vx[0] = a_p1[0];
          vx[1] = a_p1[1];
          vx[2] = 0.0;
          vx = normalize(vx);
          vec3 vy;
          vy[0] = -vx[1];
          vy[1] = vx[0];
          vy[2] = 0.0;
          vertices[2] = - vx - vy;
          vertices[1] = vx - vy;
          vertices[0] = -vertices[2];
          vertices[3] = -vertices[1];
        } else {
          lightingNormal = normalize(u_cameraZ);
          vertices[0] = u_vertices[0];
          vertices[1] = u_vertices[1];
          vertices[2] = u_vertices[2];
          vertices[3] = u_vertices[3];
        }
        position =  a_p0 + (vertices[int(a_position)] * scale);
        gl_Position = u_mvp * vec4(position, 1.0);
      } else {
        // Get the normal to the tail in camera space.
        // This allows to build a 2D rectangle around the 3D tail.
        vec3 normal = cross(u_cameraZ, normalize(a_p1 - a_p0));
        vec3 boundary = normal * scale * a_p2[0];
        if (a_position == 0.0) {
          position = a_p0 - boundary;
        } else if (a_position == 1.0) {
          position = a_p1 - boundary;
        } else if (a_position == 2.0) {
          position = a_p1 + boundary;
        } else if (a_position == 3.0) {
          position = a_p0 + boundary;
        }
        gl_Position = u_mvp * vec4(position, 1.0);
        lightingNormal = normalize(u_cameraZ);
      }
      if(!u_unshaded) {
        vec3 lightFactor = vec3(0.0,0.0,0.0);
        for(float lightIndex = 0.5; lightIndex < u_lightCount; lightIndex += 1.0) {
          float rowPos = (lightIndex) / u_lightTextureHeight;
          vec4 lightPosition = texture2D(u_lightTexture, vec2(0.125, rowPos));
          vec3 lightExtra = texture2D(u_lightTexture, vec2(0.375, rowPos)).xyz;
          vec4 lightColor = texture2D(u_lightTexture, vec2(0.625, rowPos));
          vec4 lightAmbColor = texture2D(u_lightTexture, vec2(0.875, rowPos));
          if(lightExtra.x > 1.5) {
            // Ambient light;
            float dist = length(position - vec3(lightPosition.xyz));
            float attenuationStart = lightExtra.y;
            float attenuationEnd = lightExtra.z;
            if( dist <= attenuationEnd ) {
              float attenuationDist = clamp((dist-attenuationStart), 0.001, (attenuationEnd-attenuationStart));
              float attenuationFactor = 1.0/(attenuationDist);
              lightFactor += attenuationFactor * lightAmbColor.a * lightAmbColor.rgb;
              
            }
          } else if(lightExtra.x > 0.5) {
            // Directional (sun) light;
            vec3 lightDirection = vec3(lightPosition.xyz);
            vec3 lightFactorContribution = lightColor.a * lightColor.rgb * clamp(dot(lightingNormal, lightDirection), 0.0, 1.0);
            if(lightFactorContribution.r > 1.0 || lightFactorContribution.g > 1.0 || lightFactorContribution.b > 1.0) {
              lightFactorContribution = clamp(lightFactorContribution, 0.0, 1.0);
            }
            lightFactor += lightFactorContribution + lightAmbColor.a * lightAmbColor.rgb;
          } else {
            // Omnidirectional light;
            vec3 deltaBtwn = position - lightPosition.xyz;
            float dist = length(position - vec3(lightPosition.xyz)) / 64.0 + 1.0;
            vec3 lightDirection = normalize(-deltaBtwn);
            vec3 lightFactorContribution = (lightColor.a/(pow(dist, 2.0))) * lightColor.rgb * clamp(dot(lightingNormal, lightDirection), 0.0, 1.0);
            if(lightFactorContribution.r > 1.0 || lightFactorContribution.g > 1.0 || lightFactorContribution.b > 1.0) {
              lightFactorContribution = clamp(lightFactorContribution, 0.0, 1.0);
            }
            lightFactor += lightFactorContribution + (lightAmbColor.a/(pow(dist, 2.0))) * lightAmbColor.rgb;
          }
        }
        vec4 sRGB = vec4(lightFactor, 1.0);        bvec4 cutoff = lessThan(sRGB, vec4(0.04045));        vec4 higher = pow((sRGB + vec4(0.055))/vec4(1.055), vec4(2.4));        vec4 lower = sRGB/vec4(12.92);        lightFactor = (higher * (vec4(1.0) - vec4(cutoff)) + lower * vec4(cutoff)).xyz;
        v_color.xyz *= clamp(lightFactor, 0.0, 1.0);
      }
    }
    void ribbon() {
      vec3 position;
      float left = a_leftRightTop[0] / 255.0;
      float right = a_leftRightTop[1] / 255.0;
      float top = a_leftRightTop[2] / 255.0;
      float bottom = top + 1.0;
      if (a_position == 0.0) {
        v_texcoord = vec2(right, top);
        position = a_p0;
      } else if (a_position == 1.0) {
        v_texcoord = vec2(right, bottom);
        position = a_p1;
      } else if (a_position == 2.0) {
        v_texcoord = vec2(left, bottom);
        position = a_p2;
      } else if (a_position == 3.0) {
        v_texcoord = vec2(left, top);
        position = a_p3;
      }
      v_texcoord[0] /= u_columns;
      v_texcoord[1] /= u_rows;
      v_color = a_color;
      gl_Position = u_mvp * vec4(position, 1.0);
    }
    void splat() {
      float factor = u_lifeSpan - a_health;
      int index;
      if (factor < u_intervalTimes[0]) {
        factor = factor / u_intervalTimes[0];
        index = 0;
      } else {
        factor = (factor - u_intervalTimes[0]) / u_intervalTimes[1];
        index = 1;
      }
      float cell = getCell(u_intervals[index], factor);
      float left = floor(mod(cell, u_columns));
      float top = floor(cell / u_columns);
      float right = left + 1.0;
      float bottom = top + 1.0;
      vec3 position;
      if (a_position == 0.0) {
        v_texcoord = vec2(left, top);
        position = a_p0;
      } else if (a_position == 1.0) {
        v_texcoord = vec2(left, bottom);
        position = a_p1;
      } else if (a_position == 2.0) {
        v_texcoord = vec2(right, bottom);
        position = a_p2;
      } else if (a_position == 3.0) {
        v_texcoord = vec2(right, top);
        position = a_p3;
      }
      v_texcoord[0] /= u_columns;
      v_texcoord[1] /= u_rows;
      v_color = mix(u_colors[index], u_colors[index + 1], factor) / 255.0;
      gl_Position = u_mvp * vec4(position, 1.0);
    }
    void ubersplat() {
      float factor = u_lifeSpan - a_health;
      vec4 color;
      if (factor < u_intervalTimes[0]) {
        color = mix(u_colors[0], u_colors[1], factor / u_intervalTimes[0]);
      } else if (factor < u_intervalTimes[0] + u_intervalTimes[1]) {
        color = u_colors[1];
      } else {
        color = mix(u_colors[1], u_colors[2], (factor - u_intervalTimes[0] - u_intervalTimes[1]) / u_intervalTimes[2]);
      }
      vec3 position;
      if (a_position == 0.0) {
        v_texcoord = vec2(0.0, 0.0);
        position = a_p0;
      } else if (a_position == 1.0) {
        v_texcoord = vec2(0.0, 1.0);
        position = a_p1;
      } else if (a_position == 2.0) {
        v_texcoord = vec2(1.0, 1.0);
        position = a_p2;
      } else if (a_position == 3.0) {
        v_texcoord = vec2(1.0, 0.0);
        position = a_p3;
      }
      v_color = color / 255.0;
      gl_Position = u_mvp * vec4(position, 1.0);
    }
    void main() {
      if (u_emitter == EMITTER_PARTICLE2) {
        particle2();
      } else if (u_emitter == EMITTER_RIBBON) {
        ribbon();
      } else if (u_emitter == EMITTER_SPLAT) {
        splat();
      } else if (u_emitter == EMITTER_UBERSPLAT) {
        ubersplat();
      }
    }