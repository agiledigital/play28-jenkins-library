def call(String index = '0') {
	return [
		containerTemplate(
			name: "play28-builder-${index}",
			image: 'agiledigital/play28-builder',
	    alwaysPullImage: true,
			command: 'cat',
			ttyEnabled: true
		)
	]
}