int defaultNum = 0;
ArrayList<Boid> g_boids = new ArrayList<Boid>();

void setup(){
    size(1000, 1000);
    //frameRate(30);
    for(int i=0; i<defaultNum; i++){
        g_boids.add(new Boid(random(width), random(height), g_boids.size() + 1));
    }
}

void draw(){
    background(150);
    //fill(255,50);
      //  rect(0, 0, width, height);
    for(Boid b: g_boids){
        b.flock(g_boids);
        b.update();
        b.show_self();
    }
    if(mousePressed){
        g_boids.add(new Boid(mouseX, mouseY, g_boids.size() + 1));
    }
}
