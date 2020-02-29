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


    float sep_k = 1.5;
    float sep_radius = r*5;
    float sep_angle = TWO_PI;

    float ali_k = 1.0;
    float ali_radius = 50;
    float ali_angle = TWO_PI;

    float coh_k = 1.0;
    float coh_radius = 50;
    float coh_angle = PI;


    float border_forceToCenter;

    

    Boid(float x, float y, int p_id){
        location = new PVector(x, y);
        velocity = new PVector(random(4)-2, random(4)-2);
        acceleration = new PVector(0, 0);
        id = p_id;
        maxspeed = 3;
        maxforce = 0.1;
        neighbordis = 50;
        border_forceToCenter = 50;

        sep_radius = r*2;
    }

    void flock(ArrayList<Boid> boids){
        PVector sep = separate(boids);
        PVector ali = align(boids);
        PVector coh = cohension(boids);

        sep.mult(sep_k);
        ali.mult(ali_k);
        coh.mult(coh_k);

        applyForce(sep);
        applyForce(ali);
        applyForce(coh); 

    }

    void update(){
        check_edge();
        //edgeToCenter();
        velocity.add(acceleration);
        velocity.limit(maxspeed);
        location.add(velocity);
        acceleration.mult(0);
    }

    void show_self(){
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
            if(id == other.id) continue;
            PVector loc_diff = PVector.sub(other.location, location);
            PVector this_v = velocity.copy();
            float d = PVector.dist(location, other.location);
            float normalized_dot = PVector.dot(loc_diff.normalize(), this_v.normalize());
            if(d < sep_radius && normalized_dot > include_dot){
                PVector diff = PVector.sub(location, other.location);
                diff.normalize();
                diff.div(d / sep_radius);
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
            if(id == other.id) continue;
            PVector loc_diff = PVector.sub(other.location, location);
            PVector this_v = velocity.copy();
            float d = PVector.dist(location, other.location);
            float normalized_dot = PVector.dot(loc_diff.normalize(), this_v.normalize());

            if(d < ali_radius && normalized_dot > include_dot){
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
        float include_dot = PVector.dot(new PVector(cos(0), sin(0)), new PVector(cos(coh_angle / 2), sin(coh_angle / 2)));
        
        for(Boid other: boids){
            if(id == other.id) continue;
            PVector loc_diff = PVector.sub(other.location, location);
            PVector this_v = velocity.copy();
            float d = PVector.dist(location, other.location);
            float normalized_dot = PVector.dot(loc_diff.normalize(), this_v.normalize());
            if(d < coh_radius && normalized_dot > include_dot){
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