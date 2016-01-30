### Call HTTP API
> curl -vi -X GET    http://localhost:9001/api/v1/books

> curl -iv -X GET    http://localhost:9001/api/v1/books/978-1935182757
 
> curl -iv -X PUT    http://localhost:9001/api/v1/books/978-1935182757 
> 	-H "Content-Type: application/json" 
> 	-d '{"author": "Thomas Alexandre", "title": "Aaa", "publishingDate": "2016-12-12"}'
	
>  curl -iv -X DELETE http://localhost:9001/api/v1/books/978-1935182757

>  curl -iv -X DELETE -u admin:passw0rd http://localhost:9001/api/v1/books/978-1935182757 
	

> curl -vi -X GET    http://localhost:9001/api/v1/publishers

> curl -vi -X POST   http://localhost:9001/api/v1/publishers
>     -H "Content-Type: application/json"
>     -d '{"name": "Leanpub"}'

> curl -vi -X GET    http://localhost:9001/api/v1/publishers/1

> curl -vi -X PUT    http://localhost:9001/api/v1/publishers/1
>     -H "Content-Type: application/json"
>     -d '{"name": "Leanpub"}'

> curl -vi -X DELETE http://localhost:9001/api/v1/publishers/1

> curl -vi -X GET    http://localhost:9001/api/v1/publishers/1/books

> curl -vi -X POST   http://localhost:9001/api/v1/publishers/2/books
> 	-H "Content-Type: application/json" 
> 	-d '{"isbn": "978-1935182757", "author": "Thomas Alexandre", "title": "Aaa", "publishingDate": "2016-12-12"}'

### Call HTTPS API
> curl --cacert certificate.crt -vi -X GET https://localhost:9001/api/v1/books

> http --verify=certificate.crt https://localhost:9001/api/v1/books

### Documentation
> curl --cacert certificate.crt -vi -X GET https://localhost:9001/api/v1/books

> http --verify=certificate.crt https://localhost:9001/api-docs

### Generating certificate 
> openssl 
>  	req 
>   -x509 
>	-sha256 
>	-newkey rsa:2048 
>	-keyout certificate.key 
>	-out certificate.crt 
>	-days 365 -nodes

> openssl 
>	pkcs12 
>  	-export 
> 	-in certificate.crt 
> 	-inkey certificate.key  
> 	-out server.p12 
> 	-name spray-book-catalog 
> 	-password pass:passw0rd

> openssl x509 -inform PEM -in certificate.crt > certiticate.pem

> keytool 
> 	-importkeystore 
> 	-srcstorepass passw0rd 
> 	-destkeystore spray-book-catalog.jks 
> 	-deststorepass passw0rd 
> 	-srckeystore server.p12 
> 	-srcstoretype PKCS12 
> 	-alias spray-book-catalog
