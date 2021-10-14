def call() {
	return [
		containerTemplate(
			name: 'testcontainer-postgres',
			image: 'postgres:13.3',
	    alwaysPullImage: true,
			command: 'cat',
			ttyEnabled: true,
			containerTemplate: [
				{key:"POSTGRES_PASSWORD", value:"postgres"},
				{key:"POSTGRES_USER", value:"postgres"},
			]
		)
	]
}