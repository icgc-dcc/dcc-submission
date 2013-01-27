package org.icgc.dcc.genes.cli;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class MongoValidator implements IValueValidator<MongoURI> {

  public void validate(String name, MongoURI mongoUri) throws ParameterException {
    try {
      Mongo mongo = new Mongo(mongoUri);
      try {
        Socket socket = mongo.getMongoOptions().socketFactory.createSocket();
        socket.connect(mongo.getAddress().getSocketAddress());
        socket.close();
      } catch (IOException ex) {
        throw new ParameterException("Invalid option: " + name + ": " + mongoUri + " is not accessible");
      } finally {
        mongo.close();
      }
    } catch (UnknownHostException e) {
      throw new ParameterException("Invalid option: " + name + ": " + mongoUri + " is not accessible");
    }
  }

}