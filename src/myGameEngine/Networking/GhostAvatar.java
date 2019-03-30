package myGameEngine.Networking;

import ray.rage.scene.Entity;
import ray.rage.scene.SceneNode;
import ray.rml.Vector3;

import java.util.UUID;

public class GhostAvatar {
    private UUID id;
    private SceneNode node;
    private Entity entity;

    public GhostAvatar(UUID id, Vector3 position) {
        this.id = id;
    }
// accessors and setters for id, node, entity, and position
//. . .
}
