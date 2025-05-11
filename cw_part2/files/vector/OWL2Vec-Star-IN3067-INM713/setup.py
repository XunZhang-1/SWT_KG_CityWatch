#!/usr/bin/env python

"""The setup script."""

from setuptools import setup, find_packages

with open('README.rst') as readme_file:
    readme = readme_file.read()

with open('HISTORY.rst') as history_file:
    history = history_file.read()

               
#Version 1.12.0 required instead of 1.13.0 for scipy             
requirements = ['Click>=7.0',
                'rdflib==7.0.0',
                'pyparsing==2.4.7',
                'scipy==1.12.0',
		'numpy==1.26.4',
                'gensim==4.3.1',
                'scikit-learn==1.4.0',
                'nltk==3.8.1',
                'OWLready2==0.45'
                ]
                
setup_requirements = []

test_requirements = []

setup(
    author="Jiaoyan Chen",
    author_email='chen00217@gmail.com',
    python_requires='>=3.7',
    classifiers=[
        'Development Status :: 2 - Pre-Alpha',
        'Intended Audience :: Developers',
        'License :: OSI Approved :: Apache Software License',
        'Natural Language :: English',
        'Programming Language :: Python :: 3',        
        'Programming Language :: Python :: 3.7',
        'Programming Language :: Python :: 3.8',
	'Programming Language :: Python :: 3.9',
	'Programming Language :: Python :: 3.10',
    ],
    description="Embedding OWL ontologies",
    entry_points={
        'console_scripts': [
            'owl2vec_star=owl2vec_star.cli:main',
        ],
    },
    install_requires=requirements,
    license="Apache-2.0 License",
    long_description=readme + '\n\n' + history,
    include_package_data=True,
    keywords='owl2vec_star',
    name='owl2vec_star',
    packages=find_packages(include=['owl2vec_star', 'owl2vec_star.*']),
    setup_requires=setup_requirements,
    test_suite='tests',
    tests_require=test_requirements,
    url='https://github.com/KRR-Oxford/OWL2Vec-Star',
    version='0.2.1',
    zip_safe=False,
)
