# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.5/reference/web/servlet.html)
* [Spring for RabbitMQ](https://docs.spring.io/spring-boot/4.0.5/reference/messaging/amqp.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Messaging with RabbitMQ](https://spring.io/guides/gs/messaging-rabbitmq/)

### Quick Run (Local)

1. Start RabbitMQ:
	```powershell
	docker compose up -d
	```
2. Run application:
	```powershell
	.\mvnw.cmd spring-boot:run
	```
3. Test endpoints:
	- Health: `http://localhost:8081/health`
	- Send message: `http://localhost:8081/send?message=hello`
4. RabbitMQ UI:
	- AMQP port: `localhost:5673`
	- URL: `http://localhost:15673`
	- Username: `user`
	- Password: `password`

### Quick Run (Docker)

1. Build and start all containers:
	```powershell
	docker compose up --build -d
	```
2. Test endpoints:
	- Health: `http://localhost:8081/health`
	- Send message: `http://localhost:8081/send?message=hello`
	- Received: `http://localhost:8081/received`

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.

