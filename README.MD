## How to build
* /gradlew clean build
## How to run
* /gradlew run 
## Things of note
* Requires Java 11
* Comes bundled with gradle wrapper
## API
* GET http://localhost:8080/accounts/ - list all accounts
* GET http://localhost:8080/accounts/A - fetch account A
* GET http://localhost:8080/accounts/transfer/20/from/A/to/B - transfer 20 between account A and account B
* POST http://localhost:8080/accounts/ - creates account A,  body : {"iban":"A","balance":"75"}
* PATCH http://localhost:8080/accounts/A - modifies account A, body : {"balance":"200"}
* DELETE http://localhost:8080/accounts/A - deletes account A