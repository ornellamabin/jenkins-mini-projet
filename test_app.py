# test_app.py
import unittest
import flask

class TestApp(unittest.TestCase):
    def test_flask_import(self):
        self.assertTrue(hasattr(flask, '__version__'))
        print(f"Flask version: {flask.__version__}")

if __name__ == '__main__':
    unittest.main()