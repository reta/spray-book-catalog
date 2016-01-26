#### Overview
Sample Book Catalog REST(ful) web services written in Spray framework.

### Supported REST(ful) endpoints

- Get all books: `http://localhost:9001/api/v1/books`

        curl -vi -X GET http://localhost:9001/api/v1/books

- Get book by ISBN: `http://localhost:9001/api/v1/books/{isbn}`

        curl -vi -X GET http://localhost:9001/api/v1/books/978-1935182757

- Update book by ISBN: `http://localhost:9001/api/v1/books/{isbn}`
 
        curl -vi -X PUT http://localhost:9001/api/v1/books/978-1935182757 
        	 -H "Content-Type: application/json" 
        	 -d '{"author": "Thomas Alexandre", "title": "Scala for Java Developers", "publishingDate": "2016-12-12"}'

- Remove book from the catalog by ISBN: `http://localhost:9001/api/v1/books/{isbn}`
	
        curl -vi -X DELETE http://localhost:9001/api/v1/books/978-1935182757

- Get all publishers: `http://localhost:9001/api/v1/publishers`

        curl -vi -X GET http://localhost:9001/api/v1/publishers

- Add new publisher to book catalog: `http://localhost:9001/api/v1/publishers`

        curl -vi -X POST http://localhost:9001/api/v1/publishers
            -H "Content-Type: application/json"
            -d '{"name": "Leanpub"}'

- Get publisher by its identifier: `http://localhost:9001/api/v1/publishers/{id}`

        curl -vi -X GET http://localhost:9001/api/v1/publishers/1

- Update publisher by its identifier: `http://localhost:9001/api/v1/publishers/{id}`

        curl -vi -X PUT http://localhost:9001/api/v1/publishers/1
            -H "Content-Type: application/json"
            -d '{"name": "Leanpub"}'

- Remove publisher by its identifier: `http://localhost:9001/api/v1/publishers/{id}`

        curl -vi -X DELETE http://localhost:9001/api/v1/publishers/1

- Get all books published by the publisher: `http://localhost:9001/api/v1/publishers/{id}/books`

        curl -vi -X GET http://localhost:9001/api/v1/publishers/1/books

- Add new book published by the publisher: `http://localhost:9001/api/v1/publishers/{id}/books`

        curl -vi -X POST http://localhost:9001/api/v1/publishers/2/books
            -H "Content-Type: application/json" 
            -d '{"isbn": "978-1935182757", "author": "Thomas Alexandre", "title": "Aaa", "publishingDate": "2016-12-12"}'
