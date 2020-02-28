import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class boids extends PApplet {

int defaultNum = 1000;
ArrayList<Boid> g_boids = new ArrayList<Boid>();

public void setup(){
    
    //frameRate(30);
    for(int i=0; i<defaultNum; i++){
        g_boids.add(new Boid(random(width), random(height), g_boids.size() + 1));
    }
}

public void draw(){
    background(150);
    for(Boid b: g_boids){
        b.flock(g_boids);
        b.update();
        b.show_self();
    }
    if(mousePressed){
        g_boids.add(new Boid(mouseX, mouseY, g_boids.size() + 1));
    }
}
class Boid{

    float r = 8;
    float maxspeed, maxforce;
    float neighbordis;

    PVector location;
    PVector velocity;
    PVector acceleration;
    int id;

    PVector coh_point = new PVector(0, 0);
    float coh_strength = 0;

    float sep_stress = 0;


    float sep_k = 1.5f;
    float sep_radius = r*3;
    float sep_angle = 2*PI;

    float ali_k = 1.0f;
    float ali_radius = 50;
    float ali_angle = PI/2;


    float border_forceToCenter;

    

    Boid(float x, float y, int p_id){
        location = new PVector(x, y);
        velocity = new PVector(random(4)-2, random(4)-2);
        acceleration = new PVector(0, 0);
        id = p_id;
        maxspeed = 3;
        maxforce = 0.1f;
        neighbordis = 50;
        border_forceToCenter = 50;

        sep_radius = r*2;
    }

    public void flock(ArrayList<Boid> boids){
        PVector sep = separate(boids);
        PVector ali = align(boids);
        PVector coh = cohension(boids);

        sep.mult(sep_k);
        ali.mult(ali_k);
        coh.mult(1.0f);

        applyForce(sep);
        applyForce(ali);
        applyForce(coh); 

    }

    public void update(){
        check_edge();
        //edgeToCenter();
        velocity.add(acceleration);
        velocity.limit(maxspeed);
        location.add(velocity);
        acceleration.mult(0);
    }

    public void show_self(){
        stroke(0);
        noStroke();
        //noFill();
        colorMode(HSB, 100);
        float col = map(sep_stress, 0, 25, 70, 0);
        fill(col, 100, 100);
        colorMode(RGB, 255);

        float theta = velocity.heading();
        
        pushMatrix();
        translate(location.x, location.y);
        rotate(theta);
        beginShape();
        vertex(r, 0);
        vertex(0, r/3);
        vertex(0, -r/3);
        vertex(r, 0);
        endShape();
        popMatrix();
        /*
        stroke(255, 0, 0, 60);
        strokeWeight(coh_strength / 5);
        point(coh_point.x, coh_point.y);
        */
    }

    private void applyForce(PVector force){
        acceleration.add(force);
    }

   

    private PVector separate(ArrayList<Boid> boids){
        PVector sum = new PVector(0, 0);
        int count = 0;
        float include_dot = PVector.dot(new PVector(cos(0), sin(0)), new PVector(cos(sep_angle / 2), sin(sep_angle / 2)));

        for(Boid other: boids){
            PVector loc_diff = PVector.sub(other.location, location);
            PVector this_v = velocity.copy();
            float d = PVector.dist(location, other.location);
            float normalized_dot = PVector.dot(loc_diff.normalize(), this_v.normalize());
            if(d > 0 && d < sep_radius && normalized_dot > include_dot){
                PVector diff = PVector.sub(location, other.location);
                diff.normalize();
                diff.div(d);
                sum.add(diff);
                count++;
            }
        }
        if(count>0){
            sep_stress = count;
            sum.div(count);
            sum.setMag(maxspeed);
            PVector steer = PVector.sub(sum, velocity);
            steer.limit(maxforce);
            return steer;
        }else{
            sep_stress = 0;
            return new PVector(0, 0);
        }
    }


    private PVector align(ArrayList<Boid> boids){
        PVector sum = new PVector(0, 0);
        int count = 0;
        //when ali_seeing wide is 2*PI/3(120deg). include_dot would be 0.5(not acutually, but near enough)
        float include_dot = PVector.dot(new PVector(cos(0), sin(0)), new PVector(cos(ali_angle / 2), sin(ali_angle / 2)));

        for(Boid other: boids){
            PVector loc_diff = PVector.sub(other.location, location);
            PVector this_v = velocity.copy();
            float d = PVector.dist(location, other.location);
            float normalized_dot = PVector.dot(loc_diff.normalize(), this_v.normalize());

            if(d > 0 && d < ali_radius && normalized_dot > include_dot){
                sum.add(other.velocity);
                count++;
            }
        }
        if(count > 0){
            sum.div(count);
            sum.setMag(maxspeed);

            PVector steer = PVector.sub(sum, velocity);
            steer.limit(maxforce);
            return steer;
        }else{
            return new PVector(0, 0);
        }


        
    }

    
    private PVector cohension(ArrayList<Boid> boids){
        PVector sum = new PVector(0, 0);
        int count = 0;

        for(Boid other: boids){
            float d = PVector.dist(location, other.location);
            if(d > 0 && d< neighbordis){
                sum.add(other.location);
                count++;
            }
        }
        if(count >  0){
            sum.div(count);
            coh_point = sum;
            coh_strength = count;
            return seek(sum);
        }else{
            coh_strength  = 0;
            return new PVector(0, 0);
        }
    }

    private PVector seek(PVector target){
        PVector desired = PVector.sub(target, location);
        desired.setMag(maxspeed);

        PVector steer = PVector.sub(desired, velocity);
        steer.limit(maxforce);

        return steer;
    }

    // [check_edge() or edgeToCenter() will be used.  
    //When boid come to edge, checkedge() will move them to opposite edge]
    //                        edgeToCenter will make them head to center.
    private void check_edge(){
        float x = location.x;
        float y = location.y;

        if(x < 0) location.x = width - x;
        else if(width < x) location.x = x - width;

        if(y < 0 ) location.y = height - y;
        else if(height < y) location.y = y - height;
    }

    private void edgeToCenter(){
        float x = location.x, y = location.y, fr = border_forceToCenter;
        if(x < fr || width-fr < x || y < fr || height-fr < y){
            PVector center = new PVector(width/2, height/2);
            acceleration.mult(0);
            applyForce(seek(center));
        }
    }
}
  public void settings() {  size(1000, 1000); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "boids" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}