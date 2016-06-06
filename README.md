# FxControllerBridge

Sometimes one likes to build complex JavaFX screen without decomposing them into multiple FXML files. This is especially true when using the SceneBuilder tool. The problem in this case is however that you are only allowed to have one single JavaFX controller declared, which will get the components injected and event handler invoked as they are defined in the FXML file.

In this case, this FXML controller injection bridge may come handy. This class is basically a micro FXML dependency injection framework. It allows "to bridge" the injection from the main controller into sub-controllers without the need to pass chains of component references around. When it comes to event handlers, the bridge also allows to forward the invocation.
 
Before you shout STOOOP - Yes, the choice of being able to inject into only one controller is made by design. The reason is that developers are encouraged to defined a controller per "component" (in the larger sense). The FXML files should be modular, and composed for instance using ''x:include''. While this is definitively the right thing to do, sometimes you just don't want to :)

So, that's how it's used:

```
import ....FxControllerBridge;

/**
 * Main FX Controller
 */
public class MainController {

    @FXML private MenuItem openMenu;
    @FXML private TreeView<MyTreeNode> sourceTree;
    @FXML private Button sourceDownloadButton;

    private final FxControllerBridge fxControllerBridge=new FxControllerBridge();

    /**
     * JavaFX callback to initialise the controller class.
     */
    public void initialize() {

        FxControllerBridge.debug=false;
        fxControllerBridge.interlace(this);
        // |
        // |___ All @FXML members are detected

        statusSubController=new StatusPanesSubController(fxControllerBridge);
        statusSubController.initialize();

        sourcesSubController=new SourcesSubController(fxControllerBridge);
        sourcesSubController.initialize();
    }

    @FXML 
    void onMenuOpen(ActionEvent event) { 
         fxControllerBridge.event(); 
         // |
         // |___ Forwards the event to a @BridgedFXML annotated method
         //      that has the same name, in whatever controller interlaced
         //      with the above FxControllerBridge instance.
    }
    
    @FXML void onSourceDownloadButton(ActionEvent event) { 
         fxControllerBridge.event(); 
    }

}
```

The DI is made based on the control member name or event handler method name (<3 Angular ^^), so these choices have to be unique in the group of classes the bridge bridges.

The target injection points are annotated with ``@BridgedFXML`` (instead if ``@FXML``). This is not strictly needed as the name would be enough to identify the injection point, but the annotation improves the readability in terms of recognising "what is injected where".

Setting the static ``debug`` flag to TRUE makes the class write all detections, injections and forwards to stdout.
 
```
import ....FxControllerBridge;
import ....FxControllerBridge.BridgedFXML;

 /**
  * A sub-controller...
  */
 public class SourcesSubController  {
  
     @BridgedFXML private MenuItem openMenu;
     @BridgedFXML private TreeView<TreeNode> sourceTree;
     @BridgedFXML private Button sourceDownloadButton;
     
     public SourcesSubController(FxControllerBridge fxControllerBridge) {
 
         fxControllerBridge.interlace(this);
         // |
         // |___ All @BridgedFXML members are injected based on name.
         //      The scope is the classes interlaced with the received
         //      FxControllerBridge instance.
     }
 
     public void initialize() {
     }
     
     @BridgedFXML
     private void onSourceDownloadButton()  {
         // Handle
         // The 
         // Event
     }

```

PS. The class is really tiny, have a look :)
 
### License (Short Version)

TLDR; It's free, but you use it at your own risk!

The author accepts no liability for damage caused by this tool. If you do not accept these condition then you are prohibited from using this tool. In all other respects the Apache License version 2 applies.

This program is free software; you can redistribute it and/or modify  it under the terms of the Apache License version 2 as published by the Apache Foundation. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 

