# CSC 165 - Assignment 3: Astronaut Game/Blast Off

## Overview

1. Click compile.bat. Next, find the Ethernet Adapter Ethernet IPv4 address for the computer that will host the server. Replace the IP address in the run.bat with the found IP. Click server.bat on the server machine, then click client.bat on both the machines.
2. No special requirements.
3. The objective is to blow up ships.
4. Keyboard uses wasd for movement, space to shoot a lazor.
5. Scripting is used for certain parameter initialization, primarily enemy parameters.
6. The genre is space shooter, play as astronauts pinned down by aliens. The game is effectively two-dimensional in terms of movement and interaction, with a three-dimensional camera.
7.
    - Networking: Players see the same enemies, and each other, including lazors.
    Ghosts: The other player, their lazors, and the UFOs are ghosts.
    - NPCs: The UFOs are npcs.
    - AI: The enemies move forward until they reach an orbit, then look at the players.
    - Models/UV: Astronauts, ships, and lazors are custom models with uv-mapped textures.
    - Animation: Astronauts have walk and idle animations.
    - Skybox/Terrain: Plainly visible.
    - Lighting: Astronauts have lights on them, and there’s a point light for exploding UFOs and giving the lava a glow.
    - Physics: The UFOs explode with physics.
    - Hierarchical Scene Node: Many nodes have children, including astronaut and its light, the explosion controlling node with its part children, and the ground being comprised of a few children.
    - Scripting: Initializes various parameters.
8. Sound couldn’t be implemented, it’s in the code commented because the audio manager initializes to null despite having the correct library.
9. Animation across the network.
10. The original UFO and ship design were done by Joel Sanchez, the Astronaut, platform, final UFO and lazor were done by David. The code was done in chunks by each of us, but eventually, we modified all the code at some point, the controllers were almost all done by David alone, however.
11. UFO, Astronaut, the platform, lazors, and their associated textures were created by us. The skybox was created using an open license software.
12. Open License for the skybox texture: https://github.com/wwwtyro/space-3d/blob/gh-pages/LICENSE
13. Rayman and Quake.

## Requires:

- Java Development Kit (JDK) 1.8.0_191, or whatever works...

## Setup/Run:

(Replace with IPs that work)

- Using the release.
    1. Make sure you have the correct java set in the Windows Environment
    2. Use compile.bat (Requires the JDK)
    3. (Optional) For local co-op and 'enemy' ufo's run server.bat first.
    4. Run client.bat
- Using Intellij IDE
    1. Set the correct JDK, JDK 1.8.0_191
    1. Set Run Configuration for MyGame.java
        - VM Options: -Xms1024m -Xmx2048m -Dsun.java2d.d3d=false
        - Program Arguments: 192.168.56.1 2505 UDP
    2. Set Run Configuration for NetworkingServer.java
        - VM Options: 2505 UDP
    3. Run NetworkingServer.java
    4. Run MyGame.java
