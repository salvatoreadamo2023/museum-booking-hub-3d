// Shader GLSL della sala. Sono stringhe perché Three.js le passa direttamente alla scheda video.

export const VERTICE_SCHERMO = `varying vec2 vUv; void main(){ vUv=uv; gl_Position=projectionMatrix*modelViewMatrix*vec4(position,1.); }`

// Proiezione a pavimento: caustiche, cerchi dei posti (libero/scelto/occupato) e onde quando si sceglie un posto.
export const VERTICE_PROIEZIONE = `varying vec2 vP; void main(){ vec4 w=modelMatrix*vec4(position,1.); vP=w.xz; gl_Position=projectionMatrix*viewMatrix*w; }`

export const frammentoProiezione = (maxPosti) => `
#define MAXS ${maxPosti}
uniform float uT; uniform vec3 uCol; uniform vec4 uSpots[MAXS]; uniform int uN; uniform vec4 uRip[8]; uniform vec4 uArea; uniform vec3 uHot;
varying vec2 vP;
float caust(vec2 p, float t){ vec2 q=p; float c=0.;
  for (int n=0;n<4;n++){ float fn=float(n)+1.; q+=vec2(sin(q.y*1.3+t*.7*fn), cos(q.x*1.1-t*.6*fn))*.35; c+=abs(sin(q.x*2.)*sin(q.y*2.)); }
  c/=4.; return pow(clamp(1.-c,0.,1.),6.); }
void main(){
  vec2 q=(vP-uArea.xy)/uArea.zw; float area=smoothstep(1.,.75,length(q));
  vec3 col=uCol*caust(vP*.55+vec2(20.),uT*.35)*.4*area + uCol*.05*area;
  vec3 FREE=vec3(.75,.88,1.), SEL=vec3(.18,1.,.6), OCC=vec3(1.,.28,.36);
  for (int i=0;i<MAXS;i++){ if (i>=uN) break; vec4 s=uSpots[i]; vec2 d=vP-s.xy; float r=length(d); if (r>1.2) continue;
    float ang=atan(d.y,d.x); float st=s.z; float hv=s.w;
    if (st<.5){ float ring=smoothstep(.035,0.,abs(r-.42)); float dash=step(.45,fract(ang*3.183+uT*.15)); col+=FREE*(ring*(.45+.5*hv)*mix(.5,1.,dash) + smoothstep(.42,.0,r)*.06*(1.+3.*hv)); }
    else if (st<1.5){ float ring=smoothstep(.04,0.,abs(r-.42)); float outer=smoothstep(.025,0.,abs(r-.55))*step(.5,fract(ang*2.546-uT*.5)); float fill=smoothstep(.45,.0,r)*(.25+.12*sin(uT*3.)); col+=SEL*(ring*1.4+outer*.9+fill); }
    else { float ring=smoothstep(.03,0.,abs(r-.42)); col+=OCC*ring*.55; }
  }
  { float r=length(vP-uHot.xy); float a=atan(vP.y-uHot.y,vP.x-uHot.x);
    float petals=.5+.5*sin(a*7.+uT*.6+r*4.); float disc=smoothstep(uHot.z,uHot.z-.25,r);
    col+=uCol*disc*(.25+.55*caust(vP*1.3,uT*.6))*(.6+.4*petals)+mix(uCol,vec3(1.),.5)*smoothstep(.05,0.,abs(r-uHot.z))*.9; }
  for (int k=0;k<8;k++){ vec4 rp=uRip[k]; float age=uT-rp.z; if (age<0.||age>3.) continue; float r=length(vP-rp.xy); float R=age*3.2;
    float w=smoothstep(.22,0.,abs(r-R))*exp(-age*1.3); float sp=caust(vP*1.4,uT)*smoothstep(R,R-.8,r)*smoothstep(R-1.6,R-.8,r)*exp(-age*1.5);
    col+=(rp.w>.5?vec3(1.,.3,.4):SEL)*(w*1.3+sp*.8); }
  col=clamp(col,0.,6.); if (any(isnan(col))) col=vec3(0.);
  gl_FragColor=vec4(col,1.);
}`

// Schermo sul fondo: tre animazioni diverse, una per tema (cristalli, strati geologici, fondale marino).
export const FRAMMENTO_SCHERMO = `
uniform float uT; uniform int uMode; uniform vec3 uCol; varying vec2 vUv;
vec2 h2(vec2 p){ p=vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))); return fract(sin(p)*43758.5453); }
float n2(vec2 p){ vec2 i=floor(p), f=fract(p); vec2 u=f*f*(3.-2.*f); return mix(mix(h2(i).x,h2(i+vec2(1,0)).x,u.x),mix(h2(i+vec2(0,1)).x,h2(i+vec2(1,1)).x,u.x),u.y); }
void main(){
  vec2 uv=vUv*vec2(2.6,1.); vec3 c;
  if (uMode==0){ // cristalli: celle di Voronoi che pulsano
    vec2 g=floor(uv*3.), f=fract(uv*3.); float md=8., md2=8.; vec2 mc;
    for (int y=-1;y<=1;y++) for (int x=-1;x<=1;x++){ vec2 o=vec2(x,y), p=h2(g+o); p=.5+.5*sin(uT*.6+6.2831*p); float d=length(o+p-f); if (d<md){ md2=md; md=d; mc=g+o; } else if (d<md2) md2=d; }
    float edge=smoothstep(.06,0.,md2-md); float hue=h2(mc).x; vec3 a=mix(vec3(.35,.2,.9),vec3(.2,.55,1.),hue);
    c=a*(.25+.6*pow(1.-md,3.))+vec3(.8,.85,1.)*edge*.9;
  } else if (uMode==1){ // strati geologici che scorrono
    float y=vUv.y+.06*sin(uv.x*2.+uT*.3)+.03*n2(uv*4.+uT*.1); float b=floor(y*9.); float sh=h2(vec2(b,3.)).x;
    vec3 a=mix(vec3(.45,.3,.18),vec3(.85,.65,.38),sh); a=mix(a,vec3(.2,.5,.45),step(.8,sh));
    float line=smoothstep(.02,0.,abs(fract(y*9.)-.02)); float fos=smoothstep(.12,.0,length(fract(uv*vec2(1.2,2.)+vec2(uT*.02,0.))-.5))*step(.6,h2(floor(uv*vec2(1.2,2.))).x);
    c=a*(.55+.4*n2(uv*20.))+vec3(1.)*line*.15+vec3(1.,.9,.7)*fos*.5;
  } else { // fondale: caustiche e luce dall'alto
    vec2 p=uv*3.; float s=0.; for (int i=0;i<3;i++){ p+=vec2(sin(p.y+uT*.4),cos(p.x+uT*.33)); s+=.33*sin(p.x+p.y); }
    c=mix(vec3(0.,.08,.2),vec3(.05,.45,.75),vUv.y)+vec3(.5,.9,1.)*pow(abs(s),6.)*.6;
    float ray=pow(max(0.,sin(vUv.x*14.+sin(uT*.2)*2.)),12.)*vUv.y; c+=vec3(.4,.8,1.)*ray*.25;
  }
  float vig=smoothstep(.0,.08,vUv.x)*smoothstep(1.,.92,vUv.x)*smoothstep(0.,.1,vUv.y)*smoothstep(1.,.9,vUv.y);
  gl_FragColor=vec4(c*vig*1.25,1.);
}`

// Cono di luce del proiettore: più chiaro dove la superficie è vista di fronte.
export const VERTICE_CONO = `varying float vY; varying vec3 vN; varying vec3 vV; void main(){ vY=uv.y; vN=normalize(normalMatrix*normal); vec4 mv=modelViewMatrix*vec4(position,1.); vV=normalize(-mv.xyz); gl_Position=projectionMatrix*mv; }`
export const FRAMMENTO_CONO = `uniform vec3 c; varying float vY; varying vec3 vN; varying vec3 vV; void main(){ float f=pow(abs(dot(vN,vV)),2.); float a=(.05+.1*vY)*f; gl_FragColor=vec4(mix(c,vec3(1.),.4)*a,a); }`
