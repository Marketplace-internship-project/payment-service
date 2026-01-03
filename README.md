# Marketplace Payment service

Entry point for microservice-architecture in marketplace app. Service is responsible for routing and authentication.

## 🚀 Quick Start

#### 1. Clone this repository's 'dev' branch

```bash
git clone -b dev https://github.com/Marketplace-internship-project/payment-serivce.git
cd api-gateway
```

#### 2. Run the `setup.sh` script:
```bash
chmod +x setup.sh
./setup.sh
```

#### 3. Swagger ui Docs are available at:
```
http://localhost:8080/swagger-ui.html
```

## Test scenario

Via api clients use Jwt bearer authenticatoin type
```
secret:
AFRpbjILk+NiHAXU95mFVTmPZAm8iFNL3eT9XmK4D5I=

{
  "sub": "51dda7f3-e3d4-4e7d-95fa-31557741df52",
  "role": "ADMIN"
}
```

Try this requests:

```
POST http://localhost:8080/api/v1/products
REQ
{
  "name":"beer",
  "price":2.99
}


POST http://localhost:8080/api/v1/auth/credentials
req
{
  "name":"User",
  "surname":"Userovich",
  "birthDate":"2000-01-01",
  "email":"user.userovcih@gmail.com",
  "login":"login",
  "password":"password"
}


POST http://localhost:8080/api/v1/orders


[
  {
  "productId":"68e4809b-c99a-4266-be06-cfc74dcd412e", #change for your product id
  "quantity":10
}
]


GET http://localhost:8080/api/v1/payments
```