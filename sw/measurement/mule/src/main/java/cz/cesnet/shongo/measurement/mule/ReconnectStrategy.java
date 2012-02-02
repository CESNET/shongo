package cz.cesnet.shongo.measurement.mule;

import org.mule.api.retry.RetryPolicy;
import org.mule.retry.PolicyStatus;
import org.mule.retry.policies.AbstractPolicyTemplate;

public class ReconnectStrategy extends AbstractPolicyTemplate
{

    public RetryPolicy createRetryInstance()
    {
        return new ReconnectRetryPolicy();
    }

    protected static class ReconnectRetryPolicy implements RetryPolicy
    {
        public PolicyStatus applyPolicy(Throwable cause)
        {
            try {
                System.out.println("Connecting in 5 seconds...");
                Thread.sleep(5000);
            } catch (InterruptedException e) {}
            return PolicyStatus.policyOk();
        }
    }

}
