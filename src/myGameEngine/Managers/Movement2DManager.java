package myGameEngine.Managers;

import ray.rage.scene.Node;
import ray.rml.Vector3;
import ray.rml.Vector3f;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

public class Movement2DManager {

    private Hashtable<Node, List<MovementEvent>> nodeMovementQueueTable = new Hashtable<>();
    private Hashtable<Node, Vector3> nodeDistanceMovedTable = new Hashtable<>();
    private float radiusConstraint;

    public Movement2DManager(float radiusConstraint){
        this.radiusConstraint = radiusConstraint;
    }

    public void queueMovementEvent(Node nodeToMove,float directionDegree, float distanceValue){
        getNodeMovementQueue(nodeToMove).add(new MovementEvent(directionDegree,distanceValue));
    }

    /**
     * Gets the movement queue associated with a node.
     * If there isn't a queue associated with the node in our table we create a queue.
     * @param node
     * @return Queue of movements for node
     */
    private List<MovementEvent> getNodeMovementQueue(Node node){
        if(nodeMovementQueueTable.containsKey(node)){
            return nodeMovementQueueTable.get(node);
        } else {
            List<MovementEvent> movementQueue= new LinkedList<>();
            nodeMovementQueueTable.put(node,movementQueue);
            return movementQueue;
        }
    }

    public void updateMovements(){
        nodeDistanceMovedTable.clear();
        nodeMovementQueueTable.forEach(new BiConsumer<Node, List<MovementEvent>>() {
            @Override
            public void accept(Node node, List<MovementEvent> movementEvents) {

                double xTotal = 0f;
                double y = 0f;
                double zTotal = 0f;
                for (MovementEvent movement : movementEvents) {
//                    System.out.println("Angle: " + movement.directionAngle + "\tValue: " + movement.distValue); //Debug
                    double theta = Math.toRadians(movement.getDirectionAngle());
                    xTotal = xTotal + (movement.getDistValue() * Math.sin(theta));
                    zTotal = zTotal + (movement.getDistValue() * Math.cos(theta));
                }
                Vector3 distance = Vector3f.createFrom((float) xTotal, (float) y, (float) zTotal);
                Vector3 newPosition = distance.add(node.getLocalPosition());
    
                nodeDistanceMovedTable.put(node,distance);
                //Checking if within bound && we did infact move
                if(newPosition.length() <= radiusConstraint && !newPosition.equals(node.getLocalPosition())) {
                    try {
                        node.lookAt(newPosition);
                    } catch (Exception e) {
                        System.out.println("NewPosition:\t" + newPosition);
                        System.out.println("WorldPosition:\t" + node.getWorldPosition());
                    }
                    node.setLocalPosition(newPosition);
                }
            }
        });
        nodeMovementQueueTable.clear();
    }
    
    public Hashtable<Node, Vector3> getNodeDistanceMovedTable() {
        return nodeDistanceMovedTable;
    }
    
    private class MovementEvent {
        float directionAngle;
        float distValue;

        public MovementEvent(float directionAngle, float distValue) {
            this.directionAngle = directionAngle;
            this.distValue = distValue;
        }

        public float getDirectionAngle() {
            return directionAngle;
        }

        public float getDistValue() {
            return distValue;
        }
    }
    
}