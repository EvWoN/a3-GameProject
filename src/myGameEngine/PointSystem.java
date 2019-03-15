package myGameEngine;

import ray.rage.scene.Node;
import ray.rage.scene.controllers.AbstractController;
import ray.rml.Vector3;

import java.util.LinkedList;

public class PointSystem {
    
    Node player1Node;
    private AbstractController p1Controller;
    Node player2Node;
    private AbstractController p2Controller;
    int p1Score = 0;
    int p2Score = 0;
    
    LinkedList<PointNode> pointNodeList = new LinkedList<>();
    Node parentNode;
    
    final float lengthParam = 1f;
    
    public PointSystem(Node p1Node, AbstractController p1Controller, Node p2Node, AbstractController p2Controller){
        this.player1Node = p1Node;
        this.p1Controller = p1Controller;
        this.player2Node = p2Node;
        this.p2Controller = p2Controller;
    }
    
    public String getP1Score() {
        return getOutput(p1Score)+"  "+p1Score;
    }
    
    public String getP2Score() {
        return getOutput(p2Score)+"  "+p2Score;
    }
    
    boolean endGameFlag = true;
    
    private String getOutput(int i){
        double v = pointNodeList.size() / 2.0;
        //If above half
        if( v < p1Score || v < p2Score){
            //End game animation
            if (parentNode != null && endGameFlag) {
                endGameFlag = false;
                p1Controller.addNode(parentNode);
            }
            //End game results output
            if(p1Score == p2Score){
                return "IT'S A TIE";
            } else if (p1Score > p2Score){
                if(p1Score == i){
                    return "YOU WON";
                } else return "YOU LOST";
            } else {
                if(p1Score == i){
                    return "YOU WON";
                } else return "YOU LOST";
            }
        } else return "";
    }
    
    //Accepts a planet node and adds a point to corresponding player if visited
    public void addPointNode(Node pointNode){
        pointNodeList.add(new PointNode(pointNode));
    }
    
    public void updateScores(){
        Node[] playerNodes = {player1Node,player2Node};
        for (Node playerNode : playerNodes) {
            Vector3 localPosition = playerNode.getLocalPosition();
            for (PointNode pointNode : pointNodeList) {
                if(pointNode.hasBeenVisited() == false) {
                    //Calculating the length
                    float length = pointNode.getNodeLocation().sub(localPosition).length();
                    if(length < lengthParam){
                        pointNode.setVisitedBy(playerNode);
                    }
                }
            }
        }
    }
    
    //Point node container
    private class PointNode{
        //player who visited
        Node visitedBy;
        //Actual node
        Node pointNode;
        
        public PointNode(Node node){
            parentNode = node.getParent();
            pointNode = node;
        }
        
        Vector3 getNodeLocation(){
            return pointNode.getLocalPosition();
        }
        
        boolean hasBeenVisited(){
            return !(visitedBy == null);
            
        }
    
        public void setVisitedBy(Node visitedBy) {
            if(visitedBy == player1Node){
                p1Controller.addNode(pointNode);
                p1Score++;
            } else {
                p2Controller.addNode(pointNode);
                p2Score ++;
            }
            this.visitedBy = visitedBy;
        }
    }
    
}
