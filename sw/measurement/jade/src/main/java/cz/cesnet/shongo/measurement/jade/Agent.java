package cz.cesnet.shongo.measurement.jade;

import jade.core.behaviours.OneShotBehaviour;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;
import jade.wrapper.gateway.JadeGateway;

import java.lang.reflect.InvocationTargetException;

/**
 * Agent implementation for Jade.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Agent extends jade.core.Agent {
    public static void runAgent(final String agentName, final Class agentClass) throws ControllerException, InterruptedException {
        System.out.println("Running agent '" + agentName +  "' as '" + agentClass.getSimpleName() + "'...");
        Agent agent = null;
        try {
            Class[] types = {String.class};
            agent = (Agent)agentClass.getDeclaredConstructor(types).newInstance(agentName);
        } catch (InvocationTargetException e) {
        } catch (NoSuchMethodException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        assert(agent != null);

        // using a controller agent, create a new agent in the container
        JadeGateway.execute(new OneShotBehaviour() {
            @Override
            public void action() {
                AgentContainer container = myAgent.getContainerController();
                try {
                    AgentController agentProxy = container.createNewAgent(agentName, agentClass.getName(), null);
                    agentProxy.start();
                } catch (StaleProxyException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void setup() {
        System.out.println("Started agent " + getName());

        addBehaviour(new MessageDumpBehaviour(this));
        addBehaviour(new CommandListenerBehaviour(this));
    }


}
