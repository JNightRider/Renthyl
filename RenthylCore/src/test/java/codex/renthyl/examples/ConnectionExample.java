/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.examples;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.Renthyl;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.tickets.DynamicTicketList;
import codex.renthyl.resources.tickets.TicketList;
import codex.renthyl.resources.tickets.TicketSelector;
import com.jme3.app.SimpleApplication;

/**
 *
 * @author codex
 */
public class ConnectionExample extends SimpleApplication {
    
    public static void main(String[] args) {
        new ConnectionExample().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Renthyl.initialize(this);
        
        FrameGraph fg = new FrameGraph(assetManager);
        TestPass passA = fg.add(new TestPass());
        TestPass passB = fg.add(new TestPass());
        
        // Connect ResultMap to ColorMap
        passB.getMainInputGroup().makeInput(passA.getMainOutputGroup(),
                TicketSelector.name("ResultMap"), TicketSelector.name("ColorMap"));
        // ... or ...
        passB.makeInput(passA, "ResultMap", "ColorMap");
        
        // Connect ResultMap to both ColorMap and DepthMap
        passB.getMainInputGroup().makeInput(passA.getMainOutputGroup(),
                TicketSelector.name("ResultMap"), TicketSelector.names("ColorMap", "DepthMap"));
        
        // Connect ResultMap and BumpMap to DynamicList
        passB.getInputGroup("DynamicList").makeInput(passA.getMainOutputGroup(),
                TicketSelector.names("ResultMap", "BumpMap"), TicketSelector.All);
        
        // Connect all tickets in NormalList to DynamicList
        passB.getInputGroup("DynamicList").makeInput(passA.getOutputGroup("NormalList"),
                TicketSelector.All, TicketSelector.All);
        
        // Connect the second ticket in NormalList to ColorMap
        passB.getMainInputGroup().makeInput(passA.getOutputGroup("NormalList"),
                TicketSelector.at(1), TicketSelector.name("ColorMap"));
        
    }
    
    private static class TestPass extends RenderPass {
        
        @Override
        protected void initialize(FrameGraph frameGraph) {
            
            addInput("ColorMap");
            addInput("DepthMap");
            
            addOutput("ResultMap");
            addOutput("BumpMap");
            
            addInputGroup(new DynamicTicketList("DynamicList"));
            TicketList normal = addOutputGroup(new TicketList("NormalList"));
            normal.add("Foo");
            normal.add("Run");
            normal.add("Time");
            
        }
        @Override
        protected void prepare(FGRenderContext context) {}
        @Override
        protected void execute(FGRenderContext context) {}
        @Override
        protected void reset(FGRenderContext context) {}
        @Override
        protected void cleanup(FrameGraph frameGraph) {}
        
    }
    
}
