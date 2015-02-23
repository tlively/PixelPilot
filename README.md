#### About
PixelPilot is a small Asteroids-esque game written with standard Java libraries for educational purposes.

#### Compiling
in PixelPilot directory:

```
$ javac -d bin/ src/PixelPilot.java
```

this will place all the generated class files in PixelPilot/bin

<h4>Create a JAR</h4>
In PixelPilot/bin directory

```
$ jar cmf manifest.mf PixelPilot.jar *
```

this will create a Java Archive, PixelPilot.jar, which can be run with

```
$ java -jar PixelPilot.jar
```

#### Play
goal: pilot the ship around the screen, shooting asteroids. Try to survive as long as possible and get a high score!

###### Controls

| Key | Action |
| :-- | :----- |
| w   | accelerate forward |
| a   | accelerate backward |
| s   | accelerate left |
| d   | accelerate right |
| j   | rotate counterclockwise |
| k   | rotate clockwise |
| space | fire |
| esc   | pause/unpause |
| enter | start a new game from the game over screen |

You start with 3 lives, which you can lose by running into an asteroid
or leaving the window. You can gain lives by reaching 200, 400, 800, etc.
points. shooting an asteroid is worth 10 points, with up to 6 bonus
points based on the time it took to shoot it. The number of asteroids
will rise, and so will their speed. When you die, you start back in the
middle of the screen. You will be invincible for 3 seconds, during which
you can move, but not shoot.

good luck, and have fun!
