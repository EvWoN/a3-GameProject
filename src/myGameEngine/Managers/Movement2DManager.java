package myGameEngine.Managers;

import myGameEngine.Networking.ProtocolClient;
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
    private ProtocolClient client;

    public Movement2DManager(float radiusConstraint, ProtocolClient client){
        this.radiusConstraint = radiusConstraint;
        this.client = client;
    }

    public void queueMovementEvent(Node nodeToMove, float directionDegree, float distanceValue){
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
                if(!distance.isZeroLength()) {
                    try {
                        node.lookAt(newPosition);
                    } catch (Exception e) {
                        System.out.println("Error - NewPosition:\t" + newPosition);
                        System.out.println("Error - WorldPosition:\t" + node.getWorldPosition());
                    }
                    if (!move(node,newPosition)) {
                        float bound = (newPosition.length() - radiusConstraint)/newPosition.length();
                        move(node,newPosition.mult(1-bound));
                    }
                }
            }
        });
        nodeMovementQueueTable.clear();
    }
    
    public float getRadiusConstraint() {
        return radiusConstraint;
    }
    
    public void setRadiusConstraint(float radiusConstraint) {
        this.radiusConstraint = radiusConstraint;
    }
    
    /**
     * Tries to move object within radius
     * @param node Node to be moved
     * @param newPosition position to attempt a move to
     * @return returns false if object wasn't able to be moved because of radius constraint
     */
    private boolean move(Node node, Vector3 newPosition){
        if (newPosition.length() <= radiusConstraint) {
            node.setLocalPosition(newPosition);
            if(client != null) client.sendMoveMessage(newPosition, node.getLocalForwardAxis());
            return true;
        } return false;
    }

    public void setClient(ProtocolClient client) { this.client = client; }

    
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
