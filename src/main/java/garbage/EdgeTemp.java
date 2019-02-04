//package net.clementlevallois.networkfromseedswithredis;
//
//import java.util.HashSet;
//import java.util.Objects;
//import java.util.Set;
//
//public class EdgeTemp<L> {
//
//    private double weight;
//    
//     private final Set<L> set;
//
//     public EdgeTemp(L a, L b, double weight) {
//          set = new HashSet<L>();
//          set.add(a);
//          set.add(b);
//          this.weight = weight;
//     }
//     
//     public int hashCode() {
//         return set.hashCode();
//     }
//
//    public double getWeight() {
//        return weight;
//    }
//    
//    public Set<L> getSet(){
//        return this.set;
//    }
//    
//    public void setWeight(double weight) {
//        this.weight = weight;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) {
//            return true;
//        }
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final EdgeTemp<?> other = (EdgeTemp<?>) obj;
//        if (!Objects.equals(this.set, other.set)) {
//            return false;
//        }
//        return true;
//    }
//
//
//
//}
