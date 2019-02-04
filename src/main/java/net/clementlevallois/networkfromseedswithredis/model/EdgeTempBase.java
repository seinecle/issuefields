package net.clementlevallois.networkfromseedswithredis.model;

import java.util.Objects;

public class EdgeTempBase<L> {

    private final double weight;
    private final String source;
    private final String target;

    public EdgeTempBase(String source, String target,double weight) {
        if (source == null){
            System.out.println("source is null");
        }
        if (target == null){
            System.out.println("target is null");
        }
        this.weight = weight;
        this.source = source.replace("userId:", "");
        this.target = target.replace("userId:", "");
    }

    public double getWeight() {
        return weight;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
    
    

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (int) (Double.doubleToLongBits(this.weight) ^ (Double.doubleToLongBits(this.weight) >>> 32));
        hash = 79 * hash + Objects.hashCode(this.source);
        hash = 79 * hash + Objects.hashCode(this.target);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EdgeTempBase<?> other = (EdgeTempBase<?>) obj;
        if (Double.doubleToLongBits(this.weight) != Double.doubleToLongBits(other.weight)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.target, other.target)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "EdgeTempBase{" + "weight=" + weight + ", source=" + source + ", target=" + target + '}';
    }
    

     

}
