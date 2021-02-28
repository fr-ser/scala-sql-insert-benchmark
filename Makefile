teardown:
	docker-compose down --remove-orphans --volumes --timeout=5

bootstrap: teardown
	docker-compose pull > /dev/null
	docker-compose up --detach database
	docker-compose run --rm -e SLEEP_LENGTH=2 -e TIMEOUT_LENGTH=10 wait-for-db

run: bootstrap
	@# get some empty lines
	@echo
	@echo
	sbt run -warn
	@echo
	@echo
	docker-compose down --remove-orphans --volumes --timeout=5
