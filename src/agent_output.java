// Unedited output from the AI agent.

import java.util.List;
import java.util.Map;

public class agent_output {

    private Map byMerchant;

    public Payment findPaymentById(String paymentId) {
        try {
            for (Object key : byMerchant.keySet()) {
                List payments = (List) byMerchant.get(key);
                for (Object item : payments) {
                    Payment payment = (Payment) item;
                    if (payment.id().equals(paymentId)) {
                        return payment;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}