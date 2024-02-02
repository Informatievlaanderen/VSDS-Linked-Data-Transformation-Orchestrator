---
layout: default
parent: LDIO Inputs
title: AMQP In
---

# LDIO AMQP In

***be.vlaanderen.informatievlaanderen.ldes.ldio.LdioAmqpIn***

The LDIO AMQP In listens to messages from
a [amqp queue](https://en.wikipedia.org/wiki/Advanced_Message_Queuing_Protocol).

## Config

| Property     | Description                                 | Required | Default             | Example             | Supported values                          |
|--------------|---------------------------------------------|----------|---------------------|---------------------|-------------------------------------------|
| remote-url   | URI to AMQP queue                           | Yes      | N/A                 | amqp://server:61616 | url                                       |
| queue        | Name of the queue                           | Yes      | N/A                 | quickstart-events   | String                                    |
| username     | Username used in authentication             | Yes      | N/A                 | client              | String                                    |
| password     | Password used in the authentication         | Yes      | N/A                 | secret              | String                                    |
| content-type | Content-type for received messages of queue | No       | application/n-quads | application/n-quads | Any content type supported by Apache Jena |

## Example

```yaml
      input:
        name: be.vlaanderen.informatievlaanderen.ldes.ldio.LdioAmqpIn
        config:
          remote-url: amqp://localhost:61616
          username: artemis
          password: artemis
          queue: example
          content-type: application/ld+json
```