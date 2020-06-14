package gov.gtas.services.jms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.gtas.summary.EventIdentifier;
import gov.gtas.summary.MessageAction;
import gov.gtas.summary.MessageSummary;
import gov.gtas.summary.MessageSummaryList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;

@Component
public class AdditionalProcessingMessageSender {

    private static final Logger logger = LoggerFactory.getLogger(AdditionalProcessingMessageSender.class);

    private final JmsTemplate jmsTemplateFile;

    @Autowired
    public AdditionalProcessingMessageSender(JmsTemplate jmsTemplateFile) {
        this.jmsTemplateFile = jmsTemplateFile;
    }

    public void sendFileContent(String queueName, MessageSummaryList messageSummaryList) {
        jmsTemplateFile.setDefaultDestinationName(queueName);
        EventIdentifier eventIdentifier = messageSummaryList.getEventIdentifier();
        jmsTemplateFile.send(session -> {
            ObjectMapper mapper = new ObjectMapper();
            String messageJson;
            try {
                messageJson = mapper.writer().writeValueAsString(messageSummaryList);
            } catch (JsonProcessingException e) {
                messageJson = e.getMessage();
                logger.error("ERROR WRITING JSON! NO ADDITIONAL PROCESSING!");
            }
            Message fwd = session.createObjectMessage(messageJson);
            setEventIdentifierProps(eventIdentifier, fwd);
            fwd.setStringProperty("action", messageSummaryList.getMessageAction().toString());
            fwd.setObjectProperty("countryList", messageSummaryList.getSummaryMetaData().getCountryList());
            fwd.setStringProperty("countryGroupName", messageSummaryList.getSummaryMetaData().getCountryGroupName());
            return fwd;
        });
    }


    public void sendFileContent(String queueName, org.springframework.messaging.Message<?> message, EventIdentifier eventIdentifier, MessageAction messageAction) {
        jmsTemplateFile.setDefaultDestinationName(queueName);
        jmsTemplateFile.send(session -> {
            MessageSummary messageSummary =  new MessageSummary();
            messageSummary.setRawMessage((String)message.getPayload());
            messageSummary.setEventIdentifier(eventIdentifier);
            messageSummary.setAction(messageAction);
            ObjectMapper mapper = new ObjectMapper();
            String messageJson;
            try {
                messageJson = mapper.writer().writeValueAsString(messageSummary);
            } catch (JsonProcessingException e) {
                messageJson = e.getMessage();
                messageSummary.setAction(MessageAction.ERROR);
                logger.error("ERROR WRITING JSON! NO ADDITIONAL PROCESSING!");
            }
            Message fwd = session.createObjectMessage(messageJson);
            setEventIdentifierProps(eventIdentifier, fwd);
            fwd.setStringProperty("action", messageAction.toString());
            return fwd;
        });
    }


    private void setEventIdentifierProps(EventIdentifier eventIdentifier, Message fwd) throws JMSException {
        fwd.setStringProperty("eventType", eventIdentifier.getEventType());
        fwd.setStringProperty("destCountry", eventIdentifier.getCountryDestination());
        fwd.setStringProperty("originCountry", eventIdentifier.getCountryOrigin());
        fwd.setStringProperty("identifier", eventIdentifier.getIdentifier());
        fwd.setObjectProperty("identifierList", eventIdentifier.getIdentifierArrayList());
    }
}
