package com.corneliadavis.cloudnative.connections.write;

import com.corneliadavis.cloudnative.connections.UserRepository;
import com.corneliadavis.cloudnative.connections.Connection;
import com.corneliadavis.cloudnative.connections.ConnectionRepository;
import com.corneliadavis.cloudnative.connections.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;

@RefreshScope
@RestController
public class ConnectionsWriteController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionsWriteController.class);
    private UserRepository userRepository;
    private ConnectionRepository connectionRepository;

    @Autowired
    public ConnectionsWriteController(UserRepository userRepository, ConnectionRepository connectionRepository) {
        this.userRepository = userRepository;
        this.connectionRepository = connectionRepository;
    }

    @Value("${connectionspostscontroller.url}")
    private String connectionsPostsControllerUrl;

    @RequestMapping(method = RequestMethod.POST, value="/users")
    public void newUser(@RequestBody User newUser, HttpServletResponse response) {

        logger.info("Have a new user with username " + newUser.getUsername());
        userRepository.save(newUser);

        try {
            //event
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForEntity(connectionsPostsControllerUrl+"/users", newUser, String.class);
        } catch (Exception e) {
            // for now, do nothing
            // It's a known bad that the successful delivery of this event depends on successful connection
            // to Connections' Posts, right at this moment. This will be fixed shortly.
            logger.info("[Connections] appears to have been a problem sending change event");
        }
    }

    @RequestMapping(method = RequestMethod.PUT, value="/users/{id}")
    public void updateUser(@PathVariable("id") Long userId, @RequestBody User newUser, HttpServletResponse response) {

        logger.info("Updating user with id " + userId);
        User user = userRepository.findOne(userId);
        newUser.setId(userId);
        userRepository.save(newUser);

        try {
            //event
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.put(connectionsPostsControllerUrl+"/users/"+newUser.getId(), newUser);
        } catch (Exception e) {
            // for now, do nothing
            // It's a known bad that the successful delivery of this event depends on successful connection
            // to Connections' Posts, right at this moment. This will be fixed shortly.
            logger.info("[Connections] appears to have been a problem sending change event");
        }

    }

    @RequestMapping(method = RequestMethod.POST, value="/connections")
    public void newConnection(@RequestBody Connection newConnection, HttpServletResponse response) {

        logger.info("Have a new connection: " + newConnection.getFollower() +
                    " is following " + newConnection.getFollowed());
        connectionRepository.save(newConnection);

        try {
            //event
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    connectionsPostsControllerUrl+"/connections", newConnection, String.class);
            logger.info("resp " + resp.getStatusCode());
        } catch (Exception e) {
            // for now, do nothing
            // It's a known bad that the successful delivery of this event depends on successful connection
            // to Connections' Posts, right at this moment. This will be fixed shortly.
            logger.info("[Connections] appears to have been a problem sending change event");
        }
    }

    @RequestMapping(method = RequestMethod.DELETE, value="/connections/{id}")
    public void deleteConnection(@PathVariable("id") Long connectionId, HttpServletResponse response) {

        Connection connection = connectionRepository.findOne(connectionId);

        logger.info("deleting connection: " + connection.getFollower() + " is no longer following " + connection.getFollowed());
        connectionRepository.delete(connectionId);

        try {
            //event
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.delete(connectionsPostsControllerUrl+"/connections/"+connectionId);
        } catch (Exception e) {
            // for now, do nothing
            // It's a known bad that the successful delivery of this event depends on successful connection
            // to Connections' Posts, right at this moment. This will be fixed shortly.
            logger.info("[Connections] appears to have been a problem sending change event");
        }

    }

}